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
    private final ExamRoutineRepository examRoutineRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MarkingStructureRepository markingStructureRepository;
    private final MarkingStructureComponentRepository markingStructureComponentRepository;
    private final ExamComponentRepository examComponentRepository;
    private final StudentFourthSubjectOverrideRepository studentFourthSubjectOverrideRepository;
    private final ClassSubjectGroupRepository classSubjectGroupRepository;

    public Map<String, Object> getMarkSheet(
            Integer routineId,
            Integer subjectId,
            Integer classId,
            Integer genderSectionId,
            Long sectionId,
            Integer groupId) {

        ExamRoutine routine = examRoutineRepository.findById(routineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + routineId));
        Integer examTypeId = routine.getExamType().getId();

        List<MarkingStructure> structures = groupId != null
                ? markingStructureRepository.findAllByGroupIdAndFilters(examTypeId, classId, subjectId, groupId)
                : markingStructureRepository.findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId);
        if (structures.isEmpty() && groupId != null) {
            structures = markingStructureRepository.findClassWideAndDeletedAtIsNull(examTypeId, classId, subjectId);
        }
        if (structures.isEmpty()) {
            throw new RuntimeException("No marking structure found for this exam. Please set up marking structure first.");
        }

        MarkingStructure structure = structures.get(0);
        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);
        if (components.isEmpty()) {
            throw new RuntimeException("Marking structure has no components defined.");
        }

        List<MarkSheetResponse.ComponentInfo> componentInfos = components.stream().map(c -> {
            MarkSheetResponse.ComponentInfo info = new MarkSheetResponse.ComponentInfo();
            info.setExamComponentId(c.getExamComponent().getId());
            info.setExamComponentName(c.getExamComponent().getName());
            info.setMaxMarks(c.getMaxMarks());
            return info;
        }).collect(Collectors.toList());

        MarkSheetResponse response = new MarkSheetResponse();
        response.setRoutineId(routineId);
        response.setSubjectId(subjectId);
        response.setSubjectName(structure.getSubject().getName());
        response.setExamTypeName(routine.getExamType().getName());
        response.setClassName(structure.getExamClass().getName());
        response.setTotalMarks(structure.getTotalMarks());
        response.setComponents(componentInfos);

        Integer routineAcademicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> allEnrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, routineAcademicYearId, null, genderSectionId, sectionId, groupId, null, null);

        if (allEnrollments.isEmpty()) {
            response.setStudents(new ArrayList<>());
            return Map.of("message", "No students found for the given filters", "data", response);
        }

        List<Long> allEnrollmentIds = allEnrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

        // build override map: enrollmentId -> overridden fourth subject id
        Map<Long, Integer> fourthSubjectOverrides = studentFourthSubjectOverrideRepository
                .findByEnrollmentIdIn(allEnrollmentIds).stream()
                .collect(Collectors.toMap(
                        StudentFourthSubjectOverride::getEnrollmentId,
                        o -> o.getSubject().getId()));

        // fourth subject ids for this class/group
        Set<Integer> groupFourthSubjectIds = classSubjectGroupRepository
                .findSubjectsForStudent(classId, groupId).stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsFourthSubject()))
                .map(g -> g.getSubject().getId())
                .collect(Collectors.toSet());

        // if this subject is a fourth subject, only show students whose override matches
        boolean isSubjectFourth = groupFourthSubjectIds.contains(subjectId);
        List<Enrollment> enrollments = isSubjectFourth
                ? allEnrollments.stream()
                        .filter(e -> subjectId.equals(fourthSubjectOverrides.get(e.getId())))
                        .collect(Collectors.toList())
                : allEnrollments;

        if (enrollments.isEmpty()) {
            response.setStudents(new ArrayList<>());
            String msg = isSubjectFourth
                    ? "No students have this subject assigned as their fourth subject"
                    : "No students found for the given filters";
            return Map.of("message", msg, "data", response);
        }

        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

        List<StudentMark> existingMarks = studentMarkRepository
                .findAllByEnrollmentIdInAndRoutineIdAndSubjectId(enrollmentIds, routineId, subjectId);

        Map<Long, Map<Integer, BigDecimal>> markMap = new HashMap<>();
        Map<Long, String> statusMap = new HashMap<>();
        for (StudentMark mark : existingMarks) {
            markMap.computeIfAbsent(mark.getEnrollmentId(), k -> new HashMap<>())
                    .put(mark.getExamComponent().getId(), mark.getMarksObtained());
            if (mark.getStatus() != null) {
                statusMap.put(mark.getEnrollmentId(), mark.getStatus());
            }
        }

        Map<Integer, Integer> maxMarksMap = components.stream()
                .collect(Collectors.toMap(
                        c -> c.getExamComponent().getId(),
                        MarkingStructureComponent::getMaxMarks));

        List<MarkSheetResponse.StudentMarkRow> rows = enrollments.stream().map(enrollment -> {
            MarkSheetResponse.StudentMarkRow row = new MarkSheetResponse.StudentMarkRow();
            row.setEnrollmentId(enrollment.getId());
            row.setStudentSystemId(enrollment.getStudentSystemId());
            row.setNameEnglish(enrollment.getStudent() != null ? enrollment.getStudent().getNameEnglish() : null);
            row.setClassRoll(enrollment.getClassRoll());

            Map<Integer, BigDecimal> studentMarks = markMap.getOrDefault(enrollment.getId(), new HashMap<>());

            List<MarkSheetResponse.StudentMarkRow.MarkEntry> markEntries = components.stream().map(c -> {
                MarkSheetResponse.StudentMarkRow.MarkEntry entry = new MarkSheetResponse.StudentMarkRow.MarkEntry();
                entry.setExamComponentId(c.getExamComponent().getId());
                entry.setExamComponentName(c.getExamComponent().getName());

                BigDecimal saved = studentMarks.get(c.getExamComponent().getId());
                Integer maxMarks = maxMarksMap.get(c.getExamComponent().getId());

                if (saved != null && maxMarks != null && saved.compareTo(BigDecimal.valueOf(maxMarks)) > 0) {
                    entry.setMarksObtained(null);
                } else {
                    entry.setMarksObtained(saved);
                }
                return entry;
            }).collect(Collectors.toList());

            row.setMarks(markEntries);

            String studentStatus = statusMap.get(enrollment.getId());
            row.setStatus(studentStatus);

            row.setFourthSubject(subjectId.equals(fourthSubjectOverrides.get(enrollment.getId())));

            boolean isAbsent = "ABSENT".equals(studentStatus) || "EXPELLED".equals(studentStatus);
            BigDecimal total = isAbsent ? BigDecimal.ZERO : markEntries.stream()
                    .map(e -> e.getMarksObtained() != null ? e.getMarksObtained() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            row.setTotal(total);

            return row;
        }).collect(Collectors.toList());

        rows.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));
        response.setStudents(rows);

        return Map.of("message", "Mark sheet fetched successfully", "data", response);
    }

    @Transactional
    public Map<String, Object> saveMarks(SaveMarksRequest request) {
        if (request.getRoutineId() == null) throw new RuntimeException("Routine id is required");
        if (request.getSubjectId() == null) throw new RuntimeException("Subject id is required");
        if (request.getClassId() == null) throw new RuntimeException("Class id is required");
        if (request.getMarks() == null || request.getMarks().isEmpty()) throw new RuntimeException("No marks provided");

        ExamRoutine routine = examRoutineRepository.findById(request.getRoutineId())
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + request.getRoutineId()));
        Integer examTypeId = routine.getExamType().getId();
        Integer routineId = request.getRoutineId();
        Integer subjectId = request.getSubjectId();
        Integer classId = request.getClassId();

        List<MarkingStructure> structures = markingStructureRepository
                .findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId);

        Map<Integer, Integer> maxMarksMap = new HashMap<>();
        if (!structures.isEmpty()) {
            List<MarkingStructureComponent> structureComponents =
                    markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structures.get(0));
            structureComponents.forEach(sc ->
                    maxMarksMap.put(sc.getExamComponent().getId(), sc.getMaxMarks()));
        }

        List<Long> enrollmentIds = request.getMarks().stream()
                .map(SaveMarksRequest.StudentMarkEntry::getEnrollmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<StudentMark> existingMarks = studentMarkRepository
                .findAllByEnrollmentIdInAndRoutineIdAndSubjectId(enrollmentIds, routineId, subjectId);

        Map<String, StudentMark> existingMap = existingMarks.stream()
                .collect(Collectors.toMap(
                        m -> m.getEnrollmentId() + "_" + m.getExamComponent().getId(),
                        m -> m));

        List<StudentMark> toSave = new ArrayList<>();

        for (SaveMarksRequest.StudentMarkEntry entry : request.getMarks()) {
            if (entry.getEnrollmentId() == null) throw new RuntimeException("Enrollment id is required for each mark entry");
            if (entry.getExamComponentId() == null) throw new RuntimeException("Exam component id is required for each mark entry");

            boolean isAbsentOrExpelled = "ABSENT".equals(entry.getStatus()) || "EXPELLED".equals(entry.getStatus());

            if (!isAbsentOrExpelled && entry.getMarksObtained() == null) continue;

            if (!isAbsentOrExpelled) {
                Integer maxMarks = maxMarksMap.get(entry.getExamComponentId());
                if (maxMarks != null && entry.getMarksObtained().compareTo(BigDecimal.valueOf(maxMarks)) > 0) {
                    throw new RuntimeException(
                            "Marks for enrollment " + entry.getEnrollmentId() +
                                    " component " + entry.getExamComponentId() +
                                    " cannot exceed max marks of " + maxMarks);
                }
            }

            String key = entry.getEnrollmentId() + "_" + entry.getExamComponentId();
            StudentMark mark = existingMap.get(key);

            if (mark != null) {
                mark.setMarksObtained(isAbsentOrExpelled ? null : entry.getMarksObtained());
                mark.setStatus(entry.getStatus());
                mark.setLastModifiedAt(LocalDateTime.now());
            } else {
                ExamComponent component = examComponentRepository
                        .findByIdAndDeletedAtIsNull(entry.getExamComponentId())
                        .orElseThrow(() -> new RuntimeException(
                                "Exam component not found with id: " + entry.getExamComponentId()));
                mark = new StudentMark();
                mark.setEnrollmentId(entry.getEnrollmentId());
                mark.setRoutineId(routineId);
                mark.setSubjectId(subjectId);
                mark.setExamComponent(component);
                mark.setMarksObtained(isAbsentOrExpelled ? null : entry.getMarksObtained());
                mark.setStatus(entry.getStatus());
            }

            toSave.add(mark);
        }

        studentMarkRepository.saveAll(toSave);

        return Map.of(
                "message", toSave.size() + " mark(s) saved successfully",
                "data", toSave.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    private StudentMarkResponse toResponse(StudentMark m) {
        StudentMarkResponse res = new StudentMarkResponse();
        res.setId(m.getId());
        res.setEnrollmentId(m.getEnrollmentId());
        res.setRoutineId(m.getRoutineId());
        res.setSubjectId(m.getSubjectId());
        res.setExamComponentId(m.getExamComponent().getId());
        res.setExamComponentName(m.getExamComponent().getName());
        res.setMarksObtained(m.getMarksObtained());
        res.setStatus(m.getStatus());
        return res;
    }
}
