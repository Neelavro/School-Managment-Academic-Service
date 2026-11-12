package com.example.academic_service.service;

import com.example.academic_service.entity.ExamComponent;
import com.example.academic_service.entity.MarkingStructure;
import com.example.academic_service.entity.MarkingStructureComponent;
import com.example.academic_service.entity.StudentMark;
import com.example.academic_service.entity.Subject;
import com.example.academic_service.repository.MarkingStructureComponentRepository;
import com.example.academic_service.repository.MarkingStructureRepository;
import com.example.academic_service.repository.SubSubjectClassRepository;
import com.example.academic_service.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sub-subjects (user, 2026-11-10): a co-curricular subject such as "Arts & Culture" is made of parts — Drawing,
 * Singing, … — that together make up its marks. Marks are entered per sub-subject against the parent's exam paper;
 * results only ever see the parent. This class turns sub-subject marks into parent marks so the result engine
 * needs no other change:
 * <ul>
 *   <li>a sub-subject applies only to the classes it's assigned to (user, 2026-11-12: classes don't all have the
 *       same parts); in other classes the parent is an ordinary subject;</li>
 *   <li>the parent's components = the sub-subjects' components merged by exam component (max marks added up),
 *       together with the parent's own components; no per-component pass mark — the parent passes on its own pass
 *       marks. When no assigned sub-subject has a structure for the exam, the parent's own components are used
 *       unchanged;</li>
 *   <li>the parent's mark for a component = the sum of its sub-subjects' marks for that component;</li>
 *   <li>marks entered on the parent itself win, per student and exam (user, 2026-11-12): exams marked before the
 *       sub-subjects existed keep their parent marks, and the office can enter a total straight on the parent;</li>
 *   <li>a student is absent in the parent only when absent in every sub-subject that has an entry; expelled in any
 *       sub-subject = expelled in the parent.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SubSubjects {

    private final SubjectRepository subjectRepository;
    private final MarkingStructureRepository markingStructureRepository;
    private final MarkingStructureComponentRepository markingStructureComponentRepository;
    private final SubSubjectClassRepository subSubjectClassRepository;

    /**
     * Active sub-subject id → parent id for one class: only sub-subjects assigned to the class, under active parents.
     * Empty when the class uses no sub-subjects.
     */
    public Map<Integer, Integer> parentOf(Integer classId) {
        if (classId == null) return Collections.emptyMap();
        Set<Integer> assigned = subSubjectClassRepository.findByClassId(classId).stream()
                .map(a -> a.getSubSubjectId()).collect(Collectors.toSet());
        if (assigned.isEmpty()) return Collections.emptyMap();
        List<Subject> children = subjectRepository.findAllById(assigned).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()) && c.getParentSubjectId() != null).toList();
        if (children.isEmpty()) return Collections.emptyMap();
        Set<Integer> activeParents = subjectRepository.findAllById(
                        children.stream().map(Subject::getParentSubjectId).collect(Collectors.toSet()))
                .stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).map(Subject::getId).collect(Collectors.toSet());
        Map<Integer, Integer> map = new HashMap<>();
        for (Subject c : children) if (activeParents.contains(c.getParentSubjectId())) map.put(c.getId(), c.getParentSubjectId());
        return map;
    }

    /** The parent's active sub-subjects assigned to this class, in subject order. */
    public List<Subject> childrenIn(Integer parentId, Integer classId) {
        List<Integer> ids = parentOf(classId).entrySet().stream()
                .filter(e -> e.getValue().equals(parentId)).map(Map.Entry::getKey).toList();
        if (ids.isEmpty()) return Collections.emptyList();
        List<Subject> list = new ArrayList<>(subjectRepository.findAllById(ids));
        list.sort(Comparator.comparing((Subject x) -> x.getOrderIndex() != null ? x.getOrderIndex() : Integer.MAX_VALUE)
                .thenComparing(Subject::getId));
        return list;
    }

    /** Parent id → its active sub-subject ids. */
    public static Map<Integer, List<Integer>> childrenOf(Map<Integer, Integer> parentOf) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        parentOf.forEach((child, parent) -> map.computeIfAbsent(parent, k -> new ArrayList<>()).add(child));
        return map;
    }

    /** The structure that applies to a subject for one exam/class/group: the group's own, else the class-wide one. */
    public Optional<MarkingStructure> structureFor(Integer examTypeId, Integer classId, Integer subjectId, Integer groupId) {
        List<MarkingStructure> found = groupId != null
                ? markingStructureRepository.findAllByGroupIdAndFilters(examTypeId, classId, subjectId, groupId)
                : markingStructureRepository.findClassWideAndDeletedAtIsNull(examTypeId, classId, subjectId);
        if (found.isEmpty() && groupId != null)
            found = markingStructureRepository.findClassWideAndDeletedAtIsNull(examTypeId, classId, subjectId);
        return found.stream().findFirst();
    }

    /**
     * The parent's components for results: its sub-subjects' components merged by exam component. Sub-subjects
     * with no structure for this exam are left out. The returned objects are never saved.
     */
    public List<MarkingStructureComponent> mergedComponents(MarkingStructure parentStructure, List<Integer> childIds, Integer groupId) {
        Integer examTypeId = parentStructure.getExamType().getId();
        Integer classId = parentStructure.getExamClass().getId();
        List<MarkingStructure> childStructures = childIds.stream()
                .map(id -> structureFor(examTypeId, classId, id, groupId))
                .flatMap(Optional::stream)
                .toList();
        if (childStructures.isEmpty()) return Collections.emptyList();
        Map<Integer, MarkingStructureComponent> merged = new LinkedHashMap<>();
        for (MarkingStructureComponent c : markingStructureComponentRepository.findAllByMarkingStructureInAndDeletedAtIsNull(childStructures)) {
            ExamComponent ec = c.getExamComponent();
            MarkingStructureComponent m = merged.computeIfAbsent(ec.getId(), k -> {
                MarkingStructureComponent x = new MarkingStructureComponent();
                x.setMarkingStructure(parentStructure);
                x.setExamComponent(ec);
                x.setMaxMarks(0);
                x.setPassMarks(null);
                return x;
            });
            m.setMaxMarks(m.getMaxMarks() + (c.getMaxMarks() != null ? c.getMaxMarks() : 0));
        }
        List<MarkingStructureComponent> list = new ArrayList<>(merged.values());
        list.sort(Comparator.comparing((MarkingStructureComponent c) -> c.getExamComponent().getOrderIndex() != null ? c.getExamComponent().getOrderIndex() : Integer.MAX_VALUE)
                .thenComparing(c -> c.getExamComponent().getId()));
        return list;
    }

    /**
     * A parent paper's components for results and its own mark sheet: its sub-subjects' merged components plus its
     * own (per exam component the larger max marks, no pass marks). When no sub-subject has a structure for this
     * exam, the parent's own components unchanged — exams set up before the sub-subjects work as they always did.
     */
    public List<MarkingStructureComponent> componentsFor(MarkingStructure parentStructure, List<MarkingStructureComponent> own,
                                                         List<Integer> childIds, Integer groupId) {
        if (childIds.isEmpty()) return own;
        List<MarkingStructureComponent> merged = mergedComponents(parentStructure, childIds, groupId);
        if (merged.isEmpty()) return own;
        Map<Integer, MarkingStructureComponent> byComponent = new LinkedHashMap<>();
        for (MarkingStructureComponent m : merged) byComponent.put(m.getExamComponent().getId(), m);
        for (MarkingStructureComponent c : own) {
            MarkingStructureComponent m = byComponent.get(c.getExamComponent().getId());
            int max = c.getMaxMarks() != null ? c.getMaxMarks() : 0;
            if (m == null) {
                MarkingStructureComponent x = new MarkingStructureComponent();
                x.setMarkingStructure(parentStructure);
                x.setExamComponent(c.getExamComponent());
                x.setMaxMarks(max);
                x.setPassMarks(null);
                byComponent.put(c.getExamComponent().getId(), x);
            } else if (max > m.getMaxMarks()) {
                m.setMaxMarks(max);
            }
        }
        List<MarkingStructureComponent> list = new ArrayList<>(byComponent.values());
        list.sort(Comparator.comparing((MarkingStructureComponent c) -> c.getExamComponent().getOrderIndex() != null ? c.getExamComponent().getOrderIndex() : Integer.MAX_VALUE)
                .thenComparing(c -> c.getExamComponent().getId()));
        return list;
    }

    /**
     * Replaces sub-subject marks with parent marks (see class comment). Marks of subjects that are neither parents
     * nor sub-subjects pass through unchanged, and so do a parent's own marks for a student who has them (that
     * student's sub-subject marks are then dropped). Summed parent marks are new, unsaved objects.
     */
    public static List<StudentMark> rollUp(List<StudentMark> marks, Map<Integer, Integer> parentOf) {
        if (parentOf.isEmpty() || marks.isEmpty()) return marks;
        Set<Integer> parents = new HashSet<>(parentOf.values());
        // Marks entered on the parent itself win for that student and exam.
        Set<String> parentEntered = new HashSet<>();
        for (StudentMark m : marks)
            if (parents.contains(m.getSubjectId()) && isEntered(m))
                parentEntered.add(m.getEnrollmentId() + "_" + m.getRoutineId() + "_" + m.getSubjectId());
        List<StudentMark> out = new ArrayList<>();
        // "enrollment_routine_parent" → component id → summed mark
        Map<String, Map<Integer, StudentMark>> sums = new LinkedHashMap<>();
        Map<String, String> statuses = new HashMap<>();
        Map<String, StudentMark> firstOf = new LinkedHashMap<>();
        for (StudentMark m : marks) {
            Integer parent = parentOf.get(m.getSubjectId());
            if (parent == null) {
                if (!parents.contains(m.getSubjectId())
                        || parentEntered.contains(m.getEnrollmentId() + "_" + m.getRoutineId() + "_" + m.getSubjectId()))
                    out.add(m);
                continue;
            }
            String key = m.getEnrollmentId() + "_" + m.getRoutineId() + "_" + parent;
            if (parentEntered.contains(key)) continue;
            firstOf.putIfAbsent(key, m);
            if ("EXPELLED".equals(m.getStatus())) { statuses.put(key, "EXPELLED"); continue; }
            if ("ABSENT".equals(m.getStatus())) { statuses.putIfAbsent(key, "ABSENT"); continue; }
            if (m.getMarksObtained() == null) continue;
            Map<Integer, StudentMark> byComponent = sums.computeIfAbsent(key, k -> new LinkedHashMap<>());
            StudentMark sum = byComponent.computeIfAbsent(m.getExamComponent().getId(), k -> copy(m, parent, BigDecimal.ZERO, null));
            sum.setMarksObtained(sum.getMarksObtained().add(m.getMarksObtained()));
        }
        for (Map.Entry<String, StudentMark> e : firstOf.entrySet()) {
            String key = e.getKey();
            if ("EXPELLED".equals(statuses.get(key))) {
                out.add(copy(e.getValue(), parentOf.get(e.getValue().getSubjectId()), null, "EXPELLED"));
            } else if (sums.containsKey(key)) {
                out.addAll(sums.get(key).values());
            } else if (statuses.containsKey(key)) {
                out.add(copy(e.getValue(), parentOf.get(e.getValue().getSubjectId()), null, statuses.get(key)));
            }
        }
        return out;
    }

    /** A saved mark counts as entered when it has marks or is marked absent / expelled. */
    public static boolean isEntered(StudentMark m) {
        return m.getMarksObtained() != null || "ABSENT".equals(m.getStatus()) || "EXPELLED".equals(m.getStatus());
    }

    private static StudentMark copy(StudentMark from, Integer subjectId, BigDecimal marks, String status) {
        StudentMark m = new StudentMark();
        m.setEnrollmentId(from.getEnrollmentId());
        m.setRoutineId(from.getRoutineId());
        m.setSubjectId(subjectId);
        m.setExamComponent(from.getExamComponent());
        m.setMarksObtained(marks);
        m.setStatus(status);
        return m;
    }
}
