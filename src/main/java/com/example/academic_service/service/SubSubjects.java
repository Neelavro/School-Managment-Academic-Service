package com.example.academic_service.service;

import com.example.academic_service.entity.ExamComponent;
import com.example.academic_service.entity.MarkingStructure;
import com.example.academic_service.entity.MarkingStructureComponent;
import com.example.academic_service.entity.StudentMark;
import com.example.academic_service.entity.Subject;
import com.example.academic_service.repository.MarkingStructureComponentRepository;
import com.example.academic_service.repository.MarkingStructureRepository;
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
 *   <li>the parent's components = the sub-subjects' components merged by exam component (max marks added up, no
 *       per-component pass mark — the parent passes on its own pass marks);</li>
 *   <li>the parent's mark for a component = the sum of its sub-subjects' marks for that component;</li>
 *   <li>a student is absent in the parent only when absent in every sub-subject that has an entry; expelled in any
 *       sub-subject = expelled in the parent.</li>
 * </ul>
 * A parent's own marks (entered before it had sub-subjects) are ignored while it has active sub-subjects.
 */
@Component
@RequiredArgsConstructor
public class SubSubjects {

    private final SubjectRepository subjectRepository;
    private final MarkingStructureRepository markingStructureRepository;
    private final MarkingStructureComponentRepository markingStructureComponentRepository;

    /** Active sub-subject id → parent id (active parents only). Empty when the school uses no sub-subjects. */
    public Map<Integer, Integer> parentOf() {
        List<Subject> children = subjectRepository.findByParentSubjectIdIsNotNullAndIsActiveTrue();
        if (children.isEmpty()) return Collections.emptyMap();
        Set<Integer> activeParents = subjectRepository.findAllById(
                        children.stream().map(Subject::getParentSubjectId).collect(Collectors.toSet()))
                .stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).map(Subject::getId).collect(Collectors.toSet());
        Map<Integer, Integer> map = new HashMap<>();
        for (Subject c : children) if (activeParents.contains(c.getParentSubjectId())) map.put(c.getId(), c.getParentSubjectId());
        return map;
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
     * Replaces sub-subject marks with parent marks (see class comment). Marks of subjects that are neither parents
     * nor sub-subjects pass through unchanged. The parent marks are new, unsaved objects.
     */
    public static List<StudentMark> rollUp(List<StudentMark> marks, Map<Integer, Integer> parentOf) {
        if (parentOf.isEmpty() || marks.isEmpty()) return marks;
        Set<Integer> parents = new HashSet<>(parentOf.values());
        List<StudentMark> out = new ArrayList<>();
        // "enrollment_routine_parent" → component id → summed mark
        Map<String, Map<Integer, StudentMark>> sums = new LinkedHashMap<>();
        Map<String, String> statuses = new HashMap<>();
        Map<String, StudentMark> firstOf = new HashMap<>();
        for (StudentMark m : marks) {
            Integer parent = parentOf.get(m.getSubjectId());
            if (parent == null) {
                if (!parents.contains(m.getSubjectId())) out.add(m);
                continue;
            }
            String key = m.getEnrollmentId() + "_" + m.getRoutineId() + "_" + parent;
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
