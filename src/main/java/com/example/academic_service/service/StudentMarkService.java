package com.example.academic_service.service;

import com.example.academic_service.dto.marking_dtos.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentMarkService {

    private final StudentMarkRepository studentMarkRepository;
    private final ExamSessionRepository examSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MarkingStructureRepository markingStructureRepository;
    private final MarkingStructureComponentRepository markingStructureComponentRepository;
    private final ExamComponentRepository examComponentRepository;

    public Map<String, Object> getMarkSheet(
            Integer examSessionId,
            Integer genderSectionId,
            Long sectionId,
            Integer groupId) {

        // fetch exam session
        ExamSession session = examSessionRepository.findByIdAndIsActiveTrue(examSessionId)
                .orElseThrow(() -> new RuntimeException("Exam session not found with id: " + examSessionId));

        Integer classId = session.getExamClass().getId();
        Integer subjectId = session.getSubject().getId();
        Integer examTypeId = session.getExamRoutine().getExamType().getId();
        Integer sessionGroupId = session.getGroup() != null ? session.getGroup().getId() : null;

        // resolve group
        Integer resolvedGroupId = groupId != null ? groupId : sessionGroupId;

        // fetch marking structure — group-specific first, fall back to common (null group)
        List<MarkingStructure> structures = markingStructureRepository
                .findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId, resolvedGroupId);

        if (structures.isEmpty() && resolvedGroupId != null) {
            structures = markingStructureRepository
                    .findClassWideAndDeletedAtIsNull(examTypeId, classId, subjectId);
        }

        if (structures.isEmpty()) {
            throw new RuntimeException("No marking structure found for this exam session. Please set up marking structure first.");
        }

        MarkingStructure structure = structures.get(0);
        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);

        if (components.isEmpty()) {
            throw new RuntimeException("Marking structure has no components defined.");
        }

        // fetch enrollments
        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, genderSectionId, sectionId, resolvedGroupId);

        // build component info list
        List<MarkSheetResponse.ComponentInfo> componentInfos = components.stream().map(c -> {
            MarkSheetResponse.ComponentInfo info = new MarkSheetResponse.ComponentInfo();
            info.setExamComponentId(c.getExamComponent().getId());
            info.setExamComponentName(c.getExamComponent().getName());
            info.setMaxMarks(c.getMaxMarks());
            return info;
        }).collect(Collectors.toList());

        // build response shell
        MarkSheetResponse response = new MarkSheetResponse();
        response.setExamSessionId(examSessionId);
        response.setSubjectName(session.getSubject().getName());
        response.setExamTypeName(session.getExamRoutine().getExamType().getName());
        response.setClassName(session.getExamClass().getName());
        response.setTotalMarks(structure.getTotalMarks());
        response.setComponents(componentInfos);

        if (enrollments.isEmpty()) {
            response.setStudents(new ArrayList<>());
            return Map.of("message", "No students found for the given filters", "data", response);
        }

        List<Long> enrollmentIds = enrollments.stream()
                .map(Enrollment::getId)
                .collect(Collectors.toList());

        // fetch existing marks in one query
        List<StudentMark> existingMarks = studentMarkRepository
                .findAllByEnrollmentIdInAndExamSessionIdAndDeletedAtIsNull(enrollmentIds, examSessionId);

        // group marks: enrollmentId -> componentId -> marksObtained
        Map<Long, Map<Integer, BigDecimal>> markMap = new HashMap<>();
        for (StudentMark mark : existingMarks) {
            markMap
                    .computeIfAbsent(mark.getEnrollmentId(), k -> new HashMap<>())
                    .put(mark.getExamComponent().getId(), mark.getMarksObtained());
        }

        // build max marks lookup once — outside the student loop
        Map<Integer, Integer> maxMarksMap = components.stream()
                .collect(Collectors.toMap(
                        c -> c.getExamComponent().getId(),
                        MarkingStructureComponent::getMaxMarks));

        // build student rows
        List<MarkSheetResponse.StudentMarkRow> rows = enrollments.stream().map(enrollment -> {
            MarkSheetResponse.StudentMarkRow row = new MarkSheetResponse.StudentMarkRow();
            row.setEnrollmentId(enrollment.getId());
            row.setStudentSystemId(enrollment.getStudentSystemId());
            row.setClassRoll(enrollment.getClassRoll());

            Map<Integer, BigDecimal> studentMarks = markMap.getOrDefault(enrollment.getId(), new HashMap<>());

            List<MarkSheetResponse.StudentMarkRow.MarkEntry> markEntries = components.stream().map(c -> {
                MarkSheetResponse.StudentMarkRow.MarkEntry entry = new MarkSheetResponse.StudentMarkRow.MarkEntry();
                entry.setExamComponentId(c.getExamComponent().getId());
                entry.setExamComponentName(c.getExamComponent().getName());

                BigDecimal saved = studentMarks.get(c.getExamComponent().getId());
                Integer maxMarks = maxMarksMap.get(c.getExamComponent().getId());

                // null out if saved mark exceeds current max
                if (saved != null && maxMarks != null &&
                        saved.compareTo(BigDecimal.valueOf(maxMarks)) > 0) {
                    entry.setMarksObtained(null);
                } else {
                    entry.setMarksObtained(saved);
                }

                return entry;
            }).collect(Collectors.toList());

            row.setMarks(markEntries);

            // compute total from valid entries only
            BigDecimal total = markEntries.stream()
                    .map(e -> e.getMarksObtained() != null ? e.getMarksObtained() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.setTotal(total);

            return row;
        }).collect(Collectors.toList());

        // sort by class roll
        rows.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));
        response.setStudents(rows);

        return Map.of("message", "Mark sheet fetched successfully", "data", response);
    }

    @Transactional
    public Map<String, Object> saveMarks(SaveMarksRequest request) {
        if (request.getExamSessionId() == null) {
            throw new RuntimeException("Exam session id is required");
        }
        if (request.getMarks() == null || request.getMarks().isEmpty()) {
            throw new RuntimeException("No marks provided");
        }

        ExamSession session = examSessionRepository.findByIdAndIsActiveTrue(request.getExamSessionId())
                .orElseThrow(() -> new RuntimeException("Exam session not found with id: " + request.getExamSessionId()));

        Integer classId = session.getExamClass().getId();
        Integer subjectId = session.getSubject().getId();
        Integer examTypeId = session.getExamRoutine().getExamType().getId();

        // fetch marking structure once for validation
        List<MarkingStructure> structures = markingStructureRepository
                .findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId, null);

        Map<Integer, Integer> maxMarksMap = new HashMap<>();
        if (!structures.isEmpty()) {
            List<MarkingStructureComponent> structureComponents =
                    markingStructureComponentRepository
                            .findAllByMarkingStructureAndDeletedAtIsNull(structures.get(0));
            structureComponents.forEach(sc ->
                    maxMarksMap.put(sc.getExamComponent().getId(), sc.getMaxMarks()));
        }

        // fetch all existing marks for this session in one query
        List<Long> enrollmentIds = request.getMarks().stream()
                .map(SaveMarksRequest.StudentMarkEntry::getEnrollmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<StudentMark> existingMarks = studentMarkRepository
                .findAllByEnrollmentIdInAndExamSessionIdAndDeletedAtIsNull(
                        enrollmentIds, request.getExamSessionId());

        // build lookup map: enrollmentId + componentId -> existing mark
        Map<String, StudentMark> existingMap = existingMarks.stream()
                .collect(Collectors.toMap(
                        m -> m.getEnrollmentId() + "_" + m.getExamComponent().getId(),
                        m -> m));

        List<StudentMark> toSave = new ArrayList<>();

        for (SaveMarksRequest.StudentMarkEntry entry : request.getMarks()) {
            if (entry.getEnrollmentId() == null) {
                throw new RuntimeException("Enrollment id is required for each mark entry");
            }
            if (entry.getExamComponentId() == null) {
                throw new RuntimeException("Exam component id is required for each mark entry");
            }

            // skip null marks — partial save
            if (entry.getMarksObtained() == null) continue;

            // validate max marks
            Integer maxMarks = maxMarksMap.get(entry.getExamComponentId());
            if (maxMarks != null && entry.getMarksObtained().compareTo(BigDecimal.valueOf(maxMarks)) > 0) {
                throw new RuntimeException(
                        "Marks for enrollment " + entry.getEnrollmentId() +
                                " component " + entry.getExamComponentId() +
                                " cannot exceed max marks of " + maxMarks);
            }

            String key = entry.getEnrollmentId() + "_" + entry.getExamComponentId();
            StudentMark mark = existingMap.get(key);

            if (mark != null) {
                // update existing
                mark.setMarksObtained(entry.getMarksObtained());
                mark.setLastModifiedAt(LocalDateTime.now());
            } else {
                // insert new
                ExamComponent component = examComponentRepository
                        .findByIdAndDeletedAtIsNull(entry.getExamComponentId())
                        .orElseThrow(() -> new RuntimeException(
                                "Exam component not found with id: " + entry.getExamComponentId()));
                mark = new StudentMark();
                mark.setEnrollmentId(entry.getEnrollmentId());
                mark.setExamSession(session);
                mark.setExamComponent(component);
                mark.setMarksObtained(entry.getMarksObtained());
            }

            toSave.add(mark);
        }

        studentMarkRepository.saveAll(toSave);

        return Map.of(
                "message", toSave.size() + " mark(s) saved successfully",
                "data", toSave.stream().map(this::toResponse).collect(Collectors.toList()));
    }
    // ─── helpers ────────────────────────────────────────────────────────────────

    private MarkSheetResponse buildEmptySheet(
            ExamSession session,
            MarkingStructure structure,
            List<MarkingStructureComponent> components) {
        MarkSheetResponse response = new MarkSheetResponse();
        response.setExamSessionId(session.getId());
        response.setSubjectName(session.getSubject().getName());
        response.setExamTypeName(session.getExamRoutine().getExamType().getName());
        response.setClassName(session.getExamClass().getName());
        response.setTotalMarks(structure.getTotalMarks());
        response.setComponents(components.stream().map(c -> {
            MarkSheetResponse.ComponentInfo info = new MarkSheetResponse.ComponentInfo();
            info.setExamComponentId(c.getExamComponent().getId());
            info.setExamComponentName(c.getExamComponent().getName());
            info.setMaxMarks(c.getMaxMarks());
            return info;
        }).collect(Collectors.toList()));
        response.setStudents(new ArrayList<>());
        return response;
    }

    private StudentMarkResponse toResponse(StudentMark m) {
        StudentMarkResponse res = new StudentMarkResponse();
        res.setId(m.getId());
        res.setEnrollmentId(m.getEnrollmentId());
        res.setExamSessionId(m.getExamSession().getId());
        res.setExamComponentId(m.getExamComponent().getId());
        res.setExamComponentName(m.getExamComponent().getName());
        res.setMarksObtained(m.getMarksObtained());
        return res;
    }
}