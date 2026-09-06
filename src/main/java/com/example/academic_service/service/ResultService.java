package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.Section;
import com.example.academic_service.entity.StudentGroup;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRoutineRepository examRoutineRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentMarkRepository studentMarkRepository;
    private final MarkingStructureRepository markingStructureRepository;
    private final MarkingStructureComponentRepository markingStructureComponentRepository;
    private final ClassSubjectGroupRepository classSubjectGroupRepository;
    private final GradeRepository gradeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final StudentFourthSubjectOverrideRepository studentFourthSubjectOverrideRepository;

    // ─── SESSION RESULT ──────────────────────────────────────────────────────────

    public SessionResultResponse getSessionResult(Integer routineId, Integer subjectId, Integer classId, Integer genderSectionId, Long sectionId, Integer groupId) {
        ExamRoutine routine = examRoutineRepository.findById(routineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + routineId));
        Integer examTypeId = routine.getExamType().getId();

        MarkingStructure structure = resolveMarkingStructure(examTypeId, classId, subjectId, groupId);
        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);

        Class examClass = structure.getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);

        Integer routineAcademicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, routineAcademicYearId, null, genderSectionId, sectionId, groupId, null, null);
        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

        List<StudentMark> marks = studentMarkRepository
                .findAllByEnrollmentIdInAndRoutineIdAndSubjectId(enrollmentIds, routineId, subjectId);

        Map<Long, Map<Integer, StudentMark>> markMap = new HashMap<>();
        for (StudentMark m : marks) {
            markMap.computeIfAbsent(m.getEnrollmentId(), k -> new HashMap<>())
                    .put(m.getExamComponent().getId(), m);
        }

        List<SessionResultResponse.ComponentInfo> componentInfos = components.stream().map(c -> {
            SessionResultResponse.ComponentInfo ci = new SessionResultResponse.ComponentInfo();
            ci.setExamComponentId(c.getExamComponent().getId());
            ci.setExamComponentName(c.getExamComponent().getName());
            ci.setMaxMarks(c.getMaxMarks());
            ci.setPassMarks(c.getPassMarks());
            return ci;
        }).collect(Collectors.toList());

        List<SessionResultResponse.StudentSessionResult> studentRows = enrollments.stream().map(enrollment -> {
            SessionResultResponse.StudentSessionResult row = new SessionResultResponse.StudentSessionResult();
            row.setEnrollmentId(enrollment.getId());
            row.setStudentSystemId(enrollment.getStudentSystemId());
            row.setStudentName(enrollment.getStudent() != null ? enrollment.getStudent().getNameEnglish() : null);
            row.setClassRoll(enrollment.getClassRoll());

            Map<Integer, StudentMark> studentMarkMap = markMap.getOrDefault(enrollment.getId(), Collections.emptyMap());
            boolean appeared = !studentMarkMap.isEmpty();
            row.setAppeared(appeared);

            List<SessionResultResponse.ComponentMark> componentMarks = components.stream().map(c -> {
                SessionResultResponse.ComponentMark cm = new SessionResultResponse.ComponentMark();
                cm.setExamComponentId(c.getExamComponent().getId());
                cm.setExamComponentName(c.getExamComponent().getName());
                cm.setMaxMarks(c.getMaxMarks());
                cm.setPassMarks(c.getPassMarks());
                StudentMark m = studentMarkMap.get(c.getExamComponent().getId());
                if (m != null && m.getMarksObtained() != null) {
                    cm.setMarksObtained(m.getMarksObtained());
                    boolean compPassed = c.getPassMarks() != null
                            ? m.getMarksObtained().compareTo(BigDecimal.valueOf(c.getPassMarks())) >= 0
                            : true;
                    cm.setPassed(compPassed);
                }
                return cm;
            }).collect(Collectors.toList());

            row.setComponentMarks(componentMarks);

            if (appeared) {
                BigDecimal total = componentMarks.stream()
                        .filter(cm -> cm.getMarksObtained() != null)
                        .map(SessionResultResponse.ComponentMark::getMarksObtained)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                row.setMarksObtained(total);
                row.setMaxMarks(structure.getTotalMarks());

                Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
                Map<Integer, BigDecimal> compMarkMap = new HashMap<>();
                for (SessionResultResponse.ComponentMark cm : componentMarks) {
                    if (cm.getMarksObtained() != null)
                        compMarkMap.put(cm.getExamComponentId(), cm.getMarksObtained());
                }
                boolean passed = isSubjectPassed(total, structure.getPassMarks(), grade, components, compMarkMap);
                if (!passed) grade = getFailGrade(sortedGrades);
                if (grade != null) {
                    row.setGradeName(grade.getName());
                    row.setGpaValue(grade.getGpaValue());
                }
                row.setPassed(passed);
            }
            return row;
        }).collect(Collectors.toList());

        studentRows.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));

        SessionResultResponse response = new SessionResultResponse();
        response.setRoutineId(routineId);
        response.setSubjectId(subjectId);
        response.setClassName(examClass.getName());
        response.setSubjectName(structure.getSubject().getName());
        response.setExamTypeName(routine.getExamType().getName());
        response.setExamRoutineTitle(routine.getTitle());
        response.setTotalMarks(structure.getTotalMarks());
        response.setPassMarks(structure.getPassMarks());
        response.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        response.setComponents(componentInfos);
        response.setStudents(studentRows);
        return response;
    }

    // ─── ROUTINE RESULT ──────────────────────────────────────────────────────────

    public RoutineResultResponse getRoutineResult(Integer examRoutineId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));

        List<ExamSession> rawSessions = deduplicateBySubject(examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId));
        if (rawSessions.isEmpty()) throw new RuntimeException("No exam sessions found for this routine and class");

        Class examClass = rawSessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> defaultFourthSubjectIds = loadFourthSubjectIds(classId, groupId);
        Map<Integer, Integer> mergeGroupMap = loadMergeGroupMap(classId, groupId);
        Map<Integer, Integer> mergeOrderMap = loadMergeOrderMap(classId, groupId);
        final List<ExamSession> sessions = sortSessionsByMergeOrder(rawSessions, mergeGroupMap, mergeOrderMap);

        Integer routineAcademicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        // load ALL class enrollments for rank computation
        List<Enrollment> allEnrollments = enrollmentRepository.findAllByClassIdAndFilters(classId, routineAcademicYearId, shiftId, null, null, groupId, null, null);
        SessionDataBundle bundle = loadSessionData(sessions, classId, allEnrollments);

        Map<Long, Integer> overrideMap = loadOverrideMap(allEnrollments);
        Map<Long, Set<Integer>> compulsoryMap = loadCompulsoryMap(allEnrollments);
        Set<Integer> includedFourthSubjectIds = buildIncludedFourthSubjectIds(overrideMap, compulsoryMap);

        // compute totals for every student in the class
        Map<Long, BigDecimal> totalMarksMap = new HashMap<>();
        Map<Long, Double> gpaMap = new HashMap<>();
        Map<Long, Boolean> passedMap = new HashMap<>();

        for (Enrollment enrollment : allEnrollments) {
            Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotal = BigDecimal.ZERO;
            boolean overallPassed = true;
            Map<Integer, BigDecimal> mgObtained = new HashMap<>();
            Map<Integer, Integer> mgTotalMax = new HashMap<>();
            Map<Integer, Integer> mgPassMarks = new HashMap<>();
            Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();
            Map<Integer, Map<Integer, BigDecimal>> mgCompObtained = new HashMap<>();
            Map<Integer, Map<Integer, Integer>> mgCompPassMarks = new HashMap<>();

            for (ExamSession s : sessions) {
                if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
                if (defaultFourthSubjectIds.contains(s.getSubject().getId()) && !fourthSubjectIds.contains(s.getSubject().getId())) continue;
                if (!sessionAppliesToStudent(s, enrollment)) continue;
                MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
                List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
                boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());
                Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(s.getSubject().getId());
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getSubject().getId(), Collections.emptyMap());
                boolean appeared = !compMarks.isEmpty();
                BigDecimal total = sumComponentMarks(components, compMarks);

                if (mergeGroupId != null) {
                    if (appeared) grandTotal = grandTotal.add(total);
                    mgObtained.merge(mergeGroupId, total, BigDecimal::add);
                    mgTotalMax.merge(mergeGroupId, structure.getTotalMarks(), Integer::sum);
                    if (structure.getPassMarks() != null) mgPassMarks.merge(mergeGroupId, structure.getPassMarks(), Integer::sum);
                    mgAnyAppeared.merge(mergeGroupId, appeared, Boolean::logicalOr);
                    for (MarkingStructureComponent comp : components) {
                        if (comp.getPassMarks() != null && appeared) {
                            int cid = comp.getExamComponent().getId();
                            BigDecimal cm = compMarks.getOrDefault(cid, BigDecimal.ZERO);
                            mgCompObtained.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, cm, BigDecimal::add);
                            mgCompPassMarks.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, comp.getPassMarks(), Integer::sum);
                        }
                    }
                } else if (appeared) {
                    Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
                    boolean passed = isSubjectPassed(total, structure.getPassMarks(), grade, components, compMarks);
                    if (!passed) grade = getFailGrade(sortedGrades);
                    if (!isFourth) {
                        grandTotal = grandTotal.add(total);
                        if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                        if (!passed) overallPassed = false;
                    } else if (grade != null && s.getSubject().getId().equals(overrideMap.get(enrollment.getId()))) {
                        fourthGpa = grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    overallPassed = false;
                }
            }

            if (applyMergeGroupGpas(mgObtained, mgTotalMax, mgPassMarks, mgCompObtained, mgCompPassMarks, mgAnyAppeared, sortedGrades, mandatoryGpas) > 0)
                overallPassed = false;

            totalMarksMap.put(enrollment.getId(), grandTotal);
            passedMap.put(enrollment.getId(), overallPassed);
            if (!mandatoryGpas.isEmpty())
                gpaMap.put(enrollment.getId(), round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
        }

        // compute rank maps across all scopes
        RankMaps rankMaps = computeRankMaps(allEnrollments, totalMarksMap, gpaMap);

        // build subject infos (group-level default for the column header)
        List<RoutineResultResponse.SubjectInfo> subjectInfos = sessions.stream()
                .filter(s -> bundle.sessionStructureMap.containsKey(s.getId()))
                .filter(s -> !defaultFourthSubjectIds.contains(s.getSubject().getId()) || includedFourthSubjectIds.contains(s.getSubject().getId()))
                .map(s -> {
                    MarkingStructure ms = bundle.sessionStructureMap.get(s.getId());
                    RoutineResultResponse.SubjectInfo si = new RoutineResultResponse.SubjectInfo();
                    si.setSubjectId(s.getSubject().getId());
                    si.setSubjectName(s.getSubject().getName());
                    si.setFourthSubject(defaultFourthSubjectIds.contains(s.getSubject().getId()));
                    si.setTotalMarks(ms.getTotalMarks());
                    si.setPassMarks(ms.getPassMarks());
                    return si;
                }).collect(Collectors.toList());

        // filter enrollments for display
        List<Enrollment> filteredEnrollments = allEnrollments.stream()
                .filter(e -> matchesFilter(e, genderSectionId, sectionId, groupId, startRoll, endRoll))
                .collect(Collectors.toList());

        // build student rows for filtered enrollments
        List<RoutineResultResponse.StudentResultRow> studentRows = filteredEnrollments.stream().map(enrollment -> {
            Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);
            RoutineResultResponse.StudentResultRow row = new RoutineResultResponse.StudentResultRow();
            row.setEnrollmentId(enrollment.getId());
            row.setStudentSystemId(enrollment.getStudentSystemId());
            row.setStudentName(enrollment.getStudent() != null ? enrollment.getStudent().getNameEnglish() : null);
            row.setClassRoll(enrollment.getClassRoll());
            if (enrollment.getGenderSection() != null) {
                row.setGenderSectionId(enrollment.getGenderSection().getId());
                row.setGenderSectionName(enrollment.getGenderSection().getGenderName());
            }
            if (enrollment.getSection() != null) {
                row.setSectionId(enrollment.getSection().getId());
                row.setSectionName(enrollment.getSection().getSectionName());
            }
            if (enrollment.getStudentGroup() != null) {
                row.setGroupId(enrollment.getStudentGroup().getId());
                row.setGroupName(enrollment.getStudentGroup().getGroupName());
            }
            RoutineResultResponse.Merit merit = new RoutineResultResponse.Merit();
            merit.setClassRank(rankMaps.classRankMap.get(enrollment.getId()));
            merit.setGenderSectionRank(rankMaps.genderSectionRankMap.get(enrollment.getId()));
            merit.setSectionRank(rankMaps.sectionRankMap.get(enrollment.getId()));
            merit.setGroupRank(rankMaps.groupRankMap.get(enrollment.getId()));
            row.setMerit(merit);

            List<RoutineResultResponse.SubjectResult> subjectResults = new ArrayList<>();
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotal = BigDecimal.ZERO;
            boolean overallPassed = true;
            Map<Integer, BigDecimal> mgObtained = new HashMap<>();
            Map<Integer, Integer> mgTotalMax = new HashMap<>();
            Map<Integer, Integer> mgPassMarks = new HashMap<>();
            Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();
            Map<Integer, Map<Integer, BigDecimal>> mgCompObtained = new HashMap<>();
            Map<Integer, Map<Integer, Integer>> mgCompPassMarks = new HashMap<>();

            for (ExamSession s : sessions) {
                if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
                if (defaultFourthSubjectIds.contains(s.getSubject().getId()) && !fourthSubjectIds.contains(s.getSubject().getId())) continue;
                if (!sessionAppliesToStudent(s, enrollment)) continue;
                MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
                List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
                boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());
                Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(s.getSubject().getId());
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getSubject().getId(), Collections.emptyMap());
                boolean appeared = !compMarks.isEmpty();
                BigDecimal total = sumComponentMarks(components, compMarks);

                RoutineResultResponse.SubjectResult sr = new RoutineResultResponse.SubjectResult();
                sr.setSubjectId(s.getSubject().getId());
                sr.setMaxMarks(structure.getTotalMarks());
                sr.setFourthSubject(isFourth);
                sr.setAppeared(appeared);

                if (appeared) {
                    sr.setMarksObtained(total);
                    Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
                    boolean passed = isSubjectPassed(total, structure.getPassMarks(), grade, components, compMarks);
                    if (!passed) grade = getFailGrade(sortedGrades);
                    if (grade != null) { sr.setGradeName(grade.getName()); sr.setGpaValue(grade.getGpaValue()); }
                    sr.setPassed(passed);
                    if (mergeGroupId != null) {
                        grandTotal = grandTotal.add(total);
                        mgObtained.merge(mergeGroupId, total, BigDecimal::add);
                        mgTotalMax.merge(mergeGroupId, structure.getTotalMarks(), Integer::sum);
                        if (structure.getPassMarks() != null) mgPassMarks.merge(mergeGroupId, structure.getPassMarks(), Integer::sum);
                        mgAnyAppeared.merge(mergeGroupId, true, Boolean::logicalOr);
                        for (MarkingStructureComponent comp : components) {
                            if (comp.getPassMarks() != null) {
                                int cid = comp.getExamComponent().getId();
                                BigDecimal cm = compMarks.getOrDefault(cid, BigDecimal.ZERO);
                                mgCompObtained.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, cm, BigDecimal::add);
                                mgCompPassMarks.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, comp.getPassMarks(), Integer::sum);
                            }
                        }
                    } else if (!isFourth) {
                        grandTotal = grandTotal.add(total);
                        if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                        if (!passed) overallPassed = false;
                    } else if (grade != null && s.getSubject().getId().equals(overrideMap.get(enrollment.getId()))) {
                        fourthGpa = grade.getGpaValue();
                    }
                } else if (mergeGroupId != null) {
                    mgObtained.merge(mergeGroupId, BigDecimal.ZERO, BigDecimal::add);
                    mgTotalMax.merge(mergeGroupId, structure.getTotalMarks(), Integer::sum);
                    if (structure.getPassMarks() != null) mgPassMarks.merge(mergeGroupId, structure.getPassMarks(), Integer::sum);
                    mgAnyAppeared.merge(mergeGroupId, false, Boolean::logicalOr);
                } else if (!isFourth) {
                    overallPassed = false;
                }
                subjectResults.add(sr);
            }

            if (applyMergeGroupGpas(mgObtained, mgTotalMax, mgPassMarks, mgCompObtained, mgCompPassMarks, mgAnyAppeared, sortedGrades, mandatoryGpas) > 0)
                overallPassed = false;

            row.setSubjectResults(subjectResults);
            row.setTotalMarks(grandTotal);
            row.setOverallGpa(overallPassed && !mandatoryGpas.isEmpty()
                    ? round2(computeOverallGpa(mandatoryGpas, fourthGpa)) : 0.0);
            row.setPassed(overallPassed);
            if (!overallPassed) {
                merit.setClassRank(0); merit.setGenderSectionRank(0);
                merit.setSectionRank(0); merit.setGroupRank(0);
            }
            return row;
        }).collect(Collectors.toList());

        studentRows.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));

        RoutineResultResponse response = new RoutineResultResponse();
        response.setExamRoutineId(examRoutineId);
        response.setRoutineTitle(routine.getTitle());
        response.setExamTypeName(routine.getExamType().getName());
        response.setClassName(examClass.getName());
        response.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        response.setSubjects(subjectInfos);
        response.setStudents(studentRows);
        return response;
    }

    // ─── ANNUAL RESULT ───────────────────────────────────────────────────────────

    public AnnualResultResponse getAnnualResult(Integer academicYearId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new RuntimeException("Academic year not found: " + academicYearId));

        List<ExamSession> sessions = deduplicateByRoutineAndSubject(examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId));
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this academic year and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> defaultFourthSubjectIds = loadFourthSubjectIds(classId, groupId);
        Map<Integer, Integer> mergeGroupMap = loadMergeGroupMap(classId, groupId);
        Map<Integer, Integer> mergeOrderMap = loadMergeOrderMap(classId, groupId);

        // load ALL class enrollments for rank computation
        List<Enrollment> allEnrollments = enrollmentRepository.findAllByClassIdAndFilters(classId, academicYearId, shiftId, null, null, groupId, null, null);
        AnnualDataBundle bundle = loadAnnualData(sessions, classId, allEnrollments);

        Map<Long, Integer> overrideMap = loadOverrideMap(allEnrollments);
        Map<Long, Set<Integer>> compulsoryMap = loadCompulsoryMap(allEnrollments);
        Set<Integer> includedFourthSubjectIds = buildIncludedFourthSubjectIds(overrideMap, compulsoryMap);

        Map<Integer, String> subjectNameMap = sessions.stream()
                .collect(Collectors.toMap(s -> s.getSubject().getId(), s -> s.getSubject().getName(), (a, b) -> a));
        List<Integer> orderedSubjectIds = sortSubjectIdsByMergeOrder(
                bundle.sessionsBySubject.keySet().stream()
                        .filter(sid -> !defaultFourthSubjectIds.contains(sid) || includedFourthSubjectIds.contains(sid))
                        .sorted().collect(Collectors.toList()),
                mergeGroupMap, mergeOrderMap);

        List<AnnualResultResponse.RoutineInfo> routineInfos = sessions.stream()
                .map(ExamSession::getExamRoutine)
                .collect(Collectors.toMap(ExamRoutine::getId, r -> r, (a, b) -> a))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    AnnualResultResponse.RoutineInfo ri = new AnnualResultResponse.RoutineInfo();
                    ri.setRoutineId(entry.getValue().getId());
                    ri.setRoutineTitle(entry.getValue().getTitle());
                    ri.setExamTypeName(entry.getValue().getExamType().getName());
                    return ri;
                }).collect(Collectors.toList());

        // compute totals for every student in the class
        Map<Long, BigDecimal> totalScaledMap = new HashMap<>();
        Map<Long, Double> gpaMap = new HashMap<>();
        Map<Long, Boolean> passedMap = new HashMap<>();

        for (Enrollment enrollment : allEnrollments) {
            Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotalRaw = BigDecimal.ZERO;
            int grandMaxRaw = 0;
            boolean overallPassed = true;
            Map<Integer, BigDecimal> mgObtained = new HashMap<>();
            Map<Integer, Integer> mgTotalMax = new HashMap<>();
            Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();

            for (Integer subjectId : orderedSubjectIds) {
                if (defaultFourthSubjectIds.contains(subjectId) && !fourthSubjectIds.contains(subjectId)) continue;
                if (!subjectAppliesToStudent(subjectId, bundle.sessionsBySubject, enrollment)) continue;
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null) continue;
                boolean isFourth = fourthSubjectIds.contains(subjectId);
                Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(subjectId);
                if (mergeGroupId != null) {
                    if (asd.appeared && asd.totalMax > 0) {
                        grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                        grandMaxRaw += asd.totalMax;
                        mgObtained.merge(mergeGroupId, asd.totalObtained, BigDecimal::add);
                        mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                        mgAnyAppeared.merge(mergeGroupId, true, Boolean::logicalOr);
                    } else {
                        mgObtained.merge(mergeGroupId, BigDecimal.ZERO, BigDecimal::add);
                        mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                        mgAnyAppeared.merge(mergeGroupId, false, Boolean::logicalOr);
                    }
                } else if (asd.appeared && asd.totalMax > 0) {
                    if (!isFourth) {
                        grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                        grandMaxRaw += asd.totalMax;
                        if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                        if (!asd.passed) overallPassed = false;
                    } else if (asd.grade != null && subjectId.equals(overrideMap.get(enrollment.getId()))) {
                        fourthGpa = asd.grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    overallPassed = false;
                }
            }

            if (applyMergeGroupGpas(mgObtained, mgTotalMax, Collections.emptyMap(), new HashMap<>(), new HashMap<>(), mgAnyAppeared, sortedGrades, mandatoryGpas) > 0)
                overallPassed = false;

            BigDecimal scaled = grandMaxRaw > 0
                    ? grandTotalRaw.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(grandMaxRaw), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            totalScaledMap.put(enrollment.getId(), scaled);
            passedMap.put(enrollment.getId(), overallPassed);
            if (!mandatoryGpas.isEmpty())
                gpaMap.put(enrollment.getId(), round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
        }

        // compute rank maps
        RankMaps rankMaps = computeRankMaps(allEnrollments, totalScaledMap, gpaMap);

        // group-level default for column headers
        List<AnnualResultResponse.SubjectInfo> subjectInfos = orderedSubjectIds.stream()
                .filter(sid -> bundle.sessionsBySubject.get(sid).stream().anyMatch(s -> bundle.sessionStructureMap.containsKey(s.getId())))
                .map(sid -> {
                    AnnualResultResponse.SubjectInfo si = new AnnualResultResponse.SubjectInfo();
                    si.setSubjectId(sid);
                    si.setSubjectName(subjectNameMap.get(sid));
                    si.setFourthSubject(defaultFourthSubjectIds.contains(sid));
                    return si;
                }).collect(Collectors.toList());

        // filter enrollments for display
        List<Enrollment> filteredEnrollments = allEnrollments.stream()
                .filter(e -> matchesFilter(e, genderSectionId, sectionId, groupId, startRoll, endRoll))
                .collect(Collectors.toList());

        List<AnnualResultResponse.StudentResultRow> studentRows = filteredEnrollments.stream().map(enrollment -> {
            Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);
            AnnualResultResponse.StudentResultRow row = new AnnualResultResponse.StudentResultRow();
            row.setEnrollmentId(enrollment.getId());
            row.setStudentSystemId(enrollment.getStudentSystemId());
            row.setStudentName(enrollment.getStudent() != null ? enrollment.getStudent().getNameEnglish() : null);
            row.setClassRoll(enrollment.getClassRoll());
            if (enrollment.getGenderSection() != null) {
                row.setGenderSectionId(enrollment.getGenderSection().getId());
                row.setGenderSectionName(enrollment.getGenderSection().getGenderName());
            }
            if (enrollment.getSection() != null) {
                row.setSectionId(enrollment.getSection().getId());
                row.setSectionName(enrollment.getSection().getSectionName());
            }
            if (enrollment.getStudentGroup() != null) {
                row.setGroupId(enrollment.getStudentGroup().getId());
                row.setGroupName(enrollment.getStudentGroup().getGroupName());
            }
            AnnualResultResponse.Merit merit = new AnnualResultResponse.Merit();
            merit.setClassRank(rankMaps.classRankMap.get(enrollment.getId()));
            merit.setGenderSectionRank(rankMaps.genderSectionRankMap.get(enrollment.getId()));
            merit.setSectionRank(rankMaps.sectionRankMap.get(enrollment.getId()));
            merit.setGroupRank(rankMaps.groupRankMap.get(enrollment.getId()));
            row.setMerit(merit);

            List<AnnualResultResponse.SubjectResult> subjectResults = new ArrayList<>();
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotalRaw = BigDecimal.ZERO;
            int grandMaxRaw = 0;
            boolean overallPassed = true;
            Map<Integer, BigDecimal> mgObtained = new HashMap<>();
            Map<Integer, Integer> mgTotalMax = new HashMap<>();
            Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();

            for (Integer subjectId : orderedSubjectIds) {
                if (defaultFourthSubjectIds.contains(subjectId) && !fourthSubjectIds.contains(subjectId)) continue;
                if (!subjectAppliesToStudent(subjectId, bundle.sessionsBySubject, enrollment)) continue;
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null) continue;
                boolean isFourth = fourthSubjectIds.contains(subjectId);
                Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(subjectId);

                AnnualResultResponse.SubjectResult sr = new AnnualResultResponse.SubjectResult();
                sr.setSubjectId(subjectId);
                sr.setMaxMarksRaw(asd.totalMax);
                sr.setFourthSubject(isFourth);
                sr.setAppeared(asd.appeared);
                sr.setRoutineBreakdowns(asd.routineBreakdowns.stream().map(rbd -> {
                    AnnualResultResponse.RoutineBreakdown rb = new AnnualResultResponse.RoutineBreakdown();
                    rb.setRoutineId(rbd.routineId);
                    rb.setMarksObtained(rbd.marksObtained);
                    rb.setMaxMarks(rbd.maxMarks);
                    rb.setAppeared(rbd.appeared);
                    if (rbd.grade != null) { rb.setGradeName(rbd.grade.getName()); rb.setGpaValue(rbd.grade.getGpaValue()); }
                    rb.setPassed(rbd.passed);
                    return rb;
                }).collect(Collectors.toList()));

                if (asd.appeared && asd.totalMax > 0) {
                    sr.setMarksRaw(asd.totalObtained);
                    sr.setMarksScaled(asd.scaled);
                    if (asd.grade != null) { sr.setGradeName(asd.grade.getName()); sr.setGpaValue(asd.grade.getGpaValue()); }
                    sr.setPassed(asd.passed);
                    if (mergeGroupId != null) {
                        grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                        grandMaxRaw += asd.totalMax;
                        mgObtained.merge(mergeGroupId, asd.totalObtained, BigDecimal::add);
                        mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                        mgAnyAppeared.merge(mergeGroupId, true, Boolean::logicalOr);
                    } else if (!isFourth) {
                        grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                        grandMaxRaw += asd.totalMax;
                        if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                        if (!asd.passed) overallPassed = false;
                    } else if (asd.grade != null && subjectId.equals(overrideMap.get(enrollment.getId()))) {
                        fourthGpa = asd.grade.getGpaValue();
                    }
                } else if (mergeGroupId != null) {
                    mgObtained.merge(mergeGroupId, BigDecimal.ZERO, BigDecimal::add);
                    mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                    mgAnyAppeared.merge(mergeGroupId, false, Boolean::logicalOr);
                } else if (!isFourth) {
                    overallPassed = false;
                }
                subjectResults.add(sr);
            }

            if (applyMergeGroupGpas(mgObtained, mgTotalMax, Collections.emptyMap(), new HashMap<>(), new HashMap<>(), mgAnyAppeared, sortedGrades, mandatoryGpas) > 0)
                overallPassed = false;

            Map<Integer, Boolean> mergePassMap = buildMergePassMap(mgObtained, mgTotalMax, Collections.emptyMap(), new HashMap<>(), new HashMap<>(), mgAnyAppeared, sortedGrades);
            for (AnnualResultResponse.SubjectResult sr : subjectResults) {
                Integer mgId = mergeGroupMap.get(sr.getSubjectId());
                if (mgId != null && sr.isAppeared()) sr.setPassed(mergePassMap.getOrDefault(mgId, false));
            }

            row.setSubjectResults(subjectResults);
            row.setTotalMarksRaw(grandTotalRaw);
            if (grandMaxRaw > 0) {
                row.setTotalMarksScaled(grandTotalRaw.multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(grandMaxRaw), 2, RoundingMode.HALF_UP));
            }
            row.setOverallGpa(overallPassed && !mandatoryGpas.isEmpty()
                    ? round2(computeOverallGpa(mandatoryGpas, fourthGpa)) : 0.0);
            row.setPassed(overallPassed);
            if (!overallPassed) {
                merit.setClassRank(0); merit.setGenderSectionRank(0);
                merit.setSectionRank(0); merit.setGroupRank(0);
            }
            return row;
        }).collect(Collectors.toList());

        studentRows.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));

        AnnualResultResponse response = new AnnualResultResponse();
        response.setAcademicYearId(academicYearId);
        response.setAcademicYearName(year.getYearName());
        response.setClassName(examClass.getName());
        response.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        response.setRoutines(routineInfos);
        response.setSubjects(subjectInfos);
        response.setStudents(studentRows);
        return response;
    }

    // ─── STUDENT ROUTINE RESULT ──────────────────────────────────────────────────

    @Cacheable(value = "studentResult", key = "#enrollmentId + ':' + #examRoutineId")
    public StudentRoutineResultResponse getStudentRoutineResult(Long enrollmentId, Integer examRoutineId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));

        Integer classId = enrollment.getStudentClass().getId();
        Integer groupId = enrollment.getStudentGroup() != null ? enrollment.getStudentGroup().getId() : null;

        Class examClass = enrollment.getStudentClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> defaultFourthSubjectIds = loadFourthSubjectIds(classId, groupId);
        Map<Integer, Integer> mergeGroupMap = loadMergeGroupMap(classId, groupId);
        Map<Integer, Integer> mergeOrderMap = loadMergeOrderMap(classId, groupId);
        Map<Long, Integer> overrideMap = loadOverrideMap(List.of(enrollment));
        Map<Long, Set<Integer>> compulsoryMap = loadCompulsoryMap(List.of(enrollment));
        Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);
        final List<ExamSession> sessions = sortSessionsByMergeOrder(
                deduplicateBySubject(examSessionRepository.findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId)),
                mergeGroupMap, mergeOrderMap);

        SessionDataBundle bundle = loadSessionData(sessions, classId, List.of(enrollment));

        List<StudentRoutineResultResponse.SubjectResult> subjectResults = new ArrayList<>();
        List<Double> mandatoryGpas = new ArrayList<>();
        Double fourthGpa = null;
        BigDecimal grandTotal = BigDecimal.ZERO;
        boolean overallPassed = true;
        Map<Integer, BigDecimal> mgObtained = new HashMap<>();
        Map<Integer, Integer> mgTotalMax = new HashMap<>();
        Map<Integer, Integer> mgPassMarks = new HashMap<>();
        Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();
        Map<Integer, Map<Integer, BigDecimal>> mgCompObtained = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> mgCompPassMarks = new HashMap<>();

        for (ExamSession s : sessions) {
            if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
            if (defaultFourthSubjectIds.contains(s.getSubject().getId()) && !fourthSubjectIds.contains(s.getSubject().getId())) continue;
            if (!sessionAppliesToStudent(s, enrollment)) continue;
            MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
            boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());
            Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(s.getSubject().getId());

            Map<Integer, BigDecimal> compMarks = bundle.markMap
                    .getOrDefault(enrollmentId, Collections.emptyMap())
                    .getOrDefault(s.getSubject().getId(), Collections.emptyMap());

            boolean appeared = !compMarks.isEmpty();
            BigDecimal total = sumComponentMarks(components, compMarks);

            StudentRoutineResultResponse.SubjectResult sr = new StudentRoutineResultResponse.SubjectResult();
            sr.setSubjectId(s.getSubject().getId());
            sr.setSubjectName(s.getSubject().getName());
            sr.setFourthSubject(isFourth);
            sr.setMaxMarks(structure.getTotalMarks());
            sr.setPassMarks(structure.getPassMarks());
            sr.setAppeared(appeared);

            List<StudentRoutineResultResponse.ComponentMark> componentMarks = new ArrayList<>();
            for (MarkingStructureComponent comp : components) {
                StudentRoutineResultResponse.ComponentMark cm = new StudentRoutineResultResponse.ComponentMark();
                cm.setComponentName(comp.getExamComponent().getName());
                cm.setMax(comp.getMaxMarks());
                cm.setObtained(appeared ? compMarks.get(comp.getExamComponent().getId()) : null);
                componentMarks.add(cm);
            }
            sr.setComponents(componentMarks);

            if (appeared) {
                sr.setMarksObtained(total);
                Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
                boolean passed = isSubjectPassed(total, structure.getPassMarks(), grade, components, compMarks);
                if (!passed) grade = getFailGrade(sortedGrades);
                if (grade != null) { sr.setGradeName(grade.getName()); sr.setGpaValue(grade.getGpaValue()); }
                sr.setPassed(passed);
                if (mergeGroupId != null) {
                    grandTotal = grandTotal.add(total);
                    mgObtained.merge(mergeGroupId, total, BigDecimal::add);
                    mgTotalMax.merge(mergeGroupId, structure.getTotalMarks(), Integer::sum);
                    if (structure.getPassMarks() != null) mgPassMarks.merge(mergeGroupId, structure.getPassMarks(), Integer::sum);
                    mgAnyAppeared.merge(mergeGroupId, true, Boolean::logicalOr);
                    for (MarkingStructureComponent comp : components) {
                        if (comp.getPassMarks() != null) {
                            int cid = comp.getExamComponent().getId();
                            BigDecimal cm = compMarks.getOrDefault(cid, BigDecimal.ZERO);
                            mgCompObtained.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, cm, BigDecimal::add);
                            mgCompPassMarks.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, comp.getPassMarks(), Integer::sum);
                        }
                    }
                } else if (!isFourth) {
                    grandTotal = grandTotal.add(total);
                    if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                    if (!passed) overallPassed = false;
                } else if (grade != null && s.getSubject().getId().equals(overrideMap.get(enrollment.getId()))) {
                    fourthGpa = grade.getGpaValue();
                }
            } else if (mergeGroupId != null) {
                mgObtained.merge(mergeGroupId, BigDecimal.ZERO, BigDecimal::add);
                mgTotalMax.merge(mergeGroupId, structure.getTotalMarks(), Integer::sum);
                if (structure.getPassMarks() != null) mgPassMarks.merge(mergeGroupId, structure.getPassMarks(), Integer::sum);
                mgAnyAppeared.merge(mergeGroupId, false, Boolean::logicalOr);
            } else if (!isFourth) {
                overallPassed = false;
            }
            subjectResults.add(sr);
        }

        if (applyMergeGroupGpas(mgObtained, mgTotalMax, mgPassMarks, mgCompObtained, mgCompPassMarks, mgAnyAppeared, sortedGrades, mandatoryGpas) > 0)
            overallPassed = false;

        StudentRoutineResultResponse response = new StudentRoutineResultResponse();
        response.setEnrollmentId(enrollmentId);
        response.setStudentSystemId(enrollment.getStudentSystemId());
        response.setStudentName(enrollment.getStudent() != null ? enrollment.getStudent().getNameEnglish() : null);
        response.setClassRoll(enrollment.getClassRoll());
        response.setClassName(examClass.getName());
        response.setExamRoutineId(examRoutineId);
        response.setRoutineTitle(routine.getTitle());
        response.setExamTypeName(routine.getExamType().getName());
        response.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        response.setSubjectResults(subjectResults);
        response.setTotalMarks(grandTotal);
        response.setOverallGpa(overallPassed && !mandatoryGpas.isEmpty()
                ? round2(computeOverallGpa(mandatoryGpas, fourthGpa)) : 0.0);
        response.setPassed(overallPassed);
        return response;
    }

    // ─── STUDENT ANNUAL RESULT ───────────────────────────────────────────────────

    public StudentAnnualResultResponse getStudentAnnualResult(Long enrollmentId, Integer academicYearId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new RuntimeException("Academic year not found: " + academicYearId));

        Integer classId = enrollment.getStudentClass().getId();
        Integer groupId = enrollment.getStudentGroup() != null ? enrollment.getStudentGroup().getId() : null;

        List<ExamSession> sessions = deduplicateByRoutineAndSubject(examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId));

        Class examClass = enrollment.getStudentClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> defaultFourthSubjectIds = loadFourthSubjectIds(classId, groupId);
        Map<Integer, Integer> mergeGroupMap = loadMergeGroupMap(classId, groupId);
        Map<Integer, Integer> mergeOrderMap = loadMergeOrderMap(classId, groupId);
        Map<Long, Integer> overrideMap = loadOverrideMap(List.of(enrollment));
        Map<Long, Set<Integer>> compulsoryMap = loadCompulsoryMap(List.of(enrollment));
        Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);

        AnnualDataBundle bundle = loadAnnualData(sessions, classId, List.of(enrollment));
        List<Integer> orderedSubjectIds = sortSubjectIdsByMergeOrder(
                bundle.sessionsBySubject.keySet().stream()
                        .filter(sid -> !defaultFourthSubjectIds.contains(sid) || fourthSubjectIds.contains(sid))
                        .sorted().collect(Collectors.toList()),
                mergeGroupMap, mergeOrderMap);

        Map<Integer, String> subjectNameMap = sessions.stream()
                .collect(Collectors.toMap(s -> s.getSubject().getId(), s -> s.getSubject().getName(), (a, b) -> a));

        List<StudentAnnualResultResponse.RoutineInfo> studentRoutineInfos = sessions.stream()
                .map(ExamSession::getExamRoutine)
                .collect(Collectors.toMap(ExamRoutine::getId, r -> r, (a, b) -> a))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    StudentAnnualResultResponse.RoutineInfo ri = new StudentAnnualResultResponse.RoutineInfo();
                    ri.setRoutineId(entry.getValue().getId());
                    ri.setRoutineTitle(entry.getValue().getTitle());
                    ri.setExamTypeName(entry.getValue().getExamType().getName());
                    return ri;
                }).collect(Collectors.toList());

        List<StudentAnnualResultResponse.SubjectResult> subjectResults = new ArrayList<>();
        List<Double> mandatoryGpas = new ArrayList<>();
        Double fourthGpa = null;
        BigDecimal grandTotalRaw = BigDecimal.ZERO;
        int grandMaxRaw = 0;
        boolean overallPassed = true;
        Map<Integer, BigDecimal> mgObtained = new HashMap<>();
        Map<Integer, Integer> mgTotalMax = new HashMap<>();
        Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();

        for (Integer subjectId : orderedSubjectIds) {
            if (!subjectAppliesToStudent(subjectId, bundle.sessionsBySubject, enrollment)) continue;
            AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollmentId, sortedGrades);
            if (asd == null) continue;

            boolean isFourth = fourthSubjectIds.contains(subjectId);
            Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(subjectId);
            StudentAnnualResultResponse.SubjectResult sr = new StudentAnnualResultResponse.SubjectResult();
            sr.setSubjectId(subjectId);
            sr.setSubjectName(subjectNameMap.getOrDefault(subjectId, ""));
            sr.setFourthSubject(isFourth);
            sr.setMaxMarksRaw(asd.totalMax);
            sr.setAppeared(asd.appeared);
            sr.setRoutineBreakdowns(asd.routineBreakdowns.stream().map(rbd -> {
                StudentAnnualResultResponse.RoutineBreakdown rb = new StudentAnnualResultResponse.RoutineBreakdown();
                rb.setRoutineId(rbd.routineId);
                rb.setMarksObtained(rbd.marksObtained);
                rb.setMaxMarks(rbd.maxMarks);
                rb.setAppeared(rbd.appeared);
                if (rbd.grade != null) { rb.setGradeName(rbd.grade.getName()); rb.setGpaValue(rbd.grade.getGpaValue()); }
                rb.setPassed(rbd.passed);
                return rb;
            }).collect(Collectors.toList()));

            if (asd.appeared && asd.totalMax > 0) {
                sr.setMarksRaw(asd.totalObtained);
                sr.setMarksScaled(asd.scaled);
                if (asd.grade != null) { sr.setGradeName(asd.grade.getName()); sr.setGpaValue(asd.grade.getGpaValue()); }
                sr.setPassed(asd.passed);
                if (mergeGroupId != null) {
                    grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                    grandMaxRaw += asd.totalMax;
                    mgObtained.merge(mergeGroupId, asd.totalObtained, BigDecimal::add);
                    mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                    mgAnyAppeared.merge(mergeGroupId, true, Boolean::logicalOr);
                } else if (!isFourth) {
                    grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                    grandMaxRaw += asd.totalMax;
                    if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                    if (!asd.passed) overallPassed = false;
                } else if (asd.grade != null && subjectId.equals(overrideMap.get(enrollment.getId()))) {
                    fourthGpa = asd.grade.getGpaValue();
                }
            } else if (mergeGroupId != null) {
                mgObtained.merge(mergeGroupId, BigDecimal.ZERO, BigDecimal::add);
                mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                mgAnyAppeared.merge(mergeGroupId, false, Boolean::logicalOr);
            } else if (!isFourth) {
                overallPassed = false;
            }
            subjectResults.add(sr);
        }

        if (applyMergeGroupGpas(mgObtained, mgTotalMax, Collections.emptyMap(), new HashMap<>(), new HashMap<>(), mgAnyAppeared, sortedGrades, mandatoryGpas) > 0)
            overallPassed = false;

        Map<Integer, Boolean> mergePassMap = buildMergePassMap(mgObtained, mgTotalMax, Collections.emptyMap(), new HashMap<>(), new HashMap<>(), mgAnyAppeared, sortedGrades);
        for (StudentAnnualResultResponse.SubjectResult sr : subjectResults) {
            Integer mgId = mergeGroupMap.get(sr.getSubjectId());
            if (mgId != null && sr.isAppeared()) sr.setPassed(mergePassMap.getOrDefault(mgId, false));
        }


        StudentAnnualResultResponse response = new StudentAnnualResultResponse();
        response.setEnrollmentId(enrollmentId);
        response.setStudentSystemId(enrollment.getStudentSystemId());
        response.setStudentName(enrollment.getStudent() != null ? enrollment.getStudent().getNameEnglish() : null);
        response.setClassRoll(enrollment.getClassRoll());
        response.setClassName(examClass.getName());
        response.setAcademicYearId(academicYearId);
        response.setAcademicYearName(year.getYearName());
        response.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        response.setRoutines(studentRoutineInfos);
        response.setSubjectResults(subjectResults);
        response.setTotalMarksRaw(grandTotalRaw);
        if (grandMaxRaw > 0) {
            response.setTotalMarksScaled(grandTotalRaw.multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(grandMaxRaw), 2, RoundingMode.HALF_UP));
        }
        response.setOverallGpa(overallPassed && !mandatoryGpas.isEmpty()
                ? round2(computeOverallGpa(mandatoryGpas, fourthGpa)) : 0.0);
        response.setPassed(overallPassed);
        return response;
    }

    // ─── MERIT LIST ──────────────────────────────────────────────────────────────

    public MeritListResponse getMeritList(Integer academicYearId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new RuntimeException("Academic year not found: " + academicYearId));

        List<ExamSession> sessions = deduplicateByRoutineAndSubject(examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId));
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this academic year and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> defaultFourthSubjectIds = loadFourthSubjectIds(classId, groupId);
        Map<Integer, Integer> mergeGroupMap = loadMergeGroupMap(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, academicYearId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        AnnualDataBundle bundle = loadAnnualData(sessions, classId, enrollments);
        List<Integer> orderedSubjectIds = bundle.sessionsBySubject.keySet().stream().sorted().collect(Collectors.toList());

        Map<Long, Integer> overrideMap = loadOverrideMap(enrollments);
        Map<Long, Set<Integer>> compulsoryMap = loadCompulsoryMap(enrollments);

        List<MeritListResponse.MeritEntry> entries = enrollments.stream().map(enrollment -> {
            Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);
            BigDecimal totalRaw = BigDecimal.ZERO;
            int totalMax = 0;
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            boolean passed = true;
            Map<Integer, BigDecimal> mgObtained = new HashMap<>();
            Map<Integer, Integer> mgTotalMax = new HashMap<>();
            Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();

            for (Integer subjectId : orderedSubjectIds) {
                if (defaultFourthSubjectIds.contains(subjectId) && !fourthSubjectIds.contains(subjectId)) continue;
                if (!subjectAppliesToStudent(subjectId, bundle.sessionsBySubject, enrollment)) continue;
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null) continue;
                boolean isFourth = fourthSubjectIds.contains(subjectId);
                Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(subjectId);

                if (mergeGroupId != null) {
                    if (asd.appeared && asd.totalMax > 0) {
                        totalRaw = totalRaw.add(asd.totalObtained);
                        totalMax += asd.totalMax;
                        mgObtained.merge(mergeGroupId, asd.totalObtained, BigDecimal::add);
                        mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                        mgAnyAppeared.merge(mergeGroupId, true, Boolean::logicalOr);
                    } else {
                        mgObtained.merge(mergeGroupId, BigDecimal.ZERO, BigDecimal::add);
                        mgTotalMax.merge(mergeGroupId, asd.totalMax, Integer::sum);
                        mgAnyAppeared.merge(mergeGroupId, false, Boolean::logicalOr);
                    }
                } else if (asd.appeared && asd.totalMax > 0) {
                    if (!isFourth) {
                        totalRaw = totalRaw.add(asd.totalObtained);
                        totalMax += asd.totalMax;
                        if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                        if (!asd.passed) passed = false;
                    } else if (asd.grade != null && subjectId.equals(overrideMap.get(enrollment.getId()))) {
                        fourthGpa = asd.grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    passed = false;
                }
            }

            if (applyMergeGroupGpas(mgObtained, mgTotalMax, Collections.emptyMap(), new HashMap<>(), new HashMap<>(), mgAnyAppeared, sortedGrades, mandatoryGpas) > 0)
                passed = false;

            MeritListResponse.MeritEntry entry = new MeritListResponse.MeritEntry();
            entry.setEnrollmentId(enrollment.getId());
            entry.setStudentSystemId(enrollment.getStudentSystemId());
            entry.setStudentName(enrollment.getStudent() != null ? enrollment.getStudent().getNameEnglish() : null);
            entry.setClassRoll(enrollment.getClassRoll());
            entry.setTotalMarksRaw(totalRaw);
            if (totalMax > 0) {
                entry.setTotalMarksScaled(totalRaw.multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalMax), 2, RoundingMode.HALF_UP));
            }
            entry.setOverallGpa(passed && !mandatoryGpas.isEmpty()
                    ? round2(computeOverallGpa(mandatoryGpas, fourthGpa)) : 0.0);
            entry.setPassed(passed);
            return entry;
        }).collect(Collectors.toList());

        entries.sort(Comparator.comparing(
                (MeritListResponse.MeritEntry e) -> e.getTotalMarksRaw() != null ? e.getTotalMarksRaw() : BigDecimal.ZERO,
                Comparator.reverseOrder()));

        int rank = 1;
        for (MeritListResponse.MeritEntry entry : entries) {
            if (entry.isPassed()) {
                entry.setRank(rank++);
            } else {
                entry.setRank(0);
            }
        }

        MeritListResponse response = new MeritListResponse();
        response.setAcademicYearId(academicYearId);
        response.setAcademicYearName(year.getYearName());
        response.setClassName(examClass.getName());
        response.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        response.setEntries(entries);
        return response;
    }

    // ─── OVERVIEW – ROUTINE ──────────────────────────────────────────────────────

    public ResultOverviewResponse getRoutineOverview(Integer examRoutineId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));

        List<ExamSession> sessions = deduplicateBySubject(examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId));
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this routine and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        Integer routineAcademicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, routineAcademicYearId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        SessionDataBundle bundle = loadSessionData(sessions, classId, enrollments);

        List<ResultOverviewResponse.SubjectOverview> subjects = new ArrayList<>();
        for (ExamSession s : sessions) {
            if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
            MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
            StatsAggregator agg = new StatsAggregator();

            for (Enrollment enrollment : enrollments) {
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getSubject().getId(), Collections.emptyMap());
                if (compMarks.isEmpty()) continue;
                BigDecimal total = sumComponentMarks(components, compMarks);
                Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
                agg.add(total.doubleValue(), grade != null ? grade.getName() : "F",
                        isSubjectPassed(total, structure.getPassMarks(), grade));
            }

            ResultOverviewResponse.SubjectOverview ov = new ResultOverviewResponse.SubjectOverview();
            ov.setSubjectId(s.getSubject().getId());
            ov.setSubjectName(s.getSubject().getName());
            ov.setFourthSubject(fourthSubjectIds.contains(s.getSubject().getId()));
            ov.setAppeared(agg.appeared);
            ov.setAbsent(enrollments.size() - agg.appeared);
            ov.setPassed(agg.passed);
            ov.setFailed(agg.failed);
            ov.setPassRate(agg.appeared > 0 ? round2(agg.passed * 100.0 / agg.appeared) : 0.0);
            ov.setGradeDistribution(agg.gradeDistribution);
            subjects.add(ov);
        }

        ResultOverviewResponse response = new ResultOverviewResponse();
        response.setExamRoutineId(examRoutineId);
        response.setRoutineTitle(routine.getTitle());
        response.setExamTypeName(routine.getExamType().getName());
        response.setClassName(examClass.getName());
        response.setTotalEnrolled(enrollments.size());
        response.setSubjects(subjects);
        return response;
    }

    // ─── OVERVIEW – ANNUAL ───────────────────────────────────────────────────────

    public ResultOverviewResponse getAnnualOverview(Integer academicYearId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new RuntimeException("Academic year not found: " + academicYearId));

        List<ExamSession> sessions = deduplicateByRoutineAndSubject(examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId));
        if (sessions.isEmpty()) throw new RuntimeException("No sessions found for this academic year");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, academicYearId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        AnnualDataBundle bundle = loadAnnualData(sessions, classId, enrollments);
        List<Integer> orderedSubjectIds = bundle.sessionsBySubject.keySet().stream().sorted().collect(Collectors.toList());

        Map<Integer, String> subjectNameMap = sessions.stream()
                .collect(Collectors.toMap(s -> s.getSubject().getId(), s -> s.getSubject().getName(), (a, b) -> a));

        List<ResultOverviewResponse.SubjectOverview> subjects = new ArrayList<>();
        for (Integer subjectId : orderedSubjectIds) {
            List<ExamSession> subjectSessions = bundle.sessionsBySubject.get(subjectId).stream()
                    .filter(s -> bundle.sessionStructureMap.containsKey(s.getId()))
                    .collect(Collectors.toList());
            if (subjectSessions.isEmpty()) continue;

            MarkingStructure refStructure = bundle.sessionStructureMap.get(subjectSessions.get(0).getId());
            StatsAggregator agg = new StatsAggregator();

            for (Enrollment enrollment : enrollments) {
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null || !asd.appeared || asd.totalMax == 0) continue;
                agg.add(asd.scaled.doubleValue(), asd.grade != null ? asd.grade.getName() : "F", asd.passed);
            }

            ResultOverviewResponse.SubjectOverview ov = new ResultOverviewResponse.SubjectOverview();
            ov.setSubjectId(subjectId);
            ov.setSubjectName(subjectNameMap.getOrDefault(subjectId, ""));
            ov.setFourthSubject(fourthSubjectIds.contains(subjectId));
            ov.setAppeared(agg.appeared);
            ov.setAbsent(enrollments.size() - agg.appeared);
            ov.setPassed(agg.passed);
            ov.setFailed(agg.failed);
            ov.setPassRate(agg.appeared > 0 ? round2(agg.passed * 100.0 / agg.appeared) : 0.0);
            ov.setGradeDistribution(agg.gradeDistribution);
            subjects.add(ov);
        }

        ResultOverviewResponse response = new ResultOverviewResponse();
        response.setAcademicYearId(academicYearId);
        response.setAcademicYearName(year.getYearName());
        response.setClassName(examClass.getName());
        response.setTotalEnrolled(enrollments.size());
        response.setSubjects(subjects);
        return response;
    }

    // ─── STATS – SESSION ─────────────────────────────────────────────────────────

    public SessionStatsResponse getSessionStats(Integer routineId, Integer subjectId, Integer classId, Integer genderSectionId, Long sectionId, Integer groupId) {
        ExamRoutine routine = examRoutineRepository.findById(routineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + routineId));
        Integer examTypeId = routine.getExamType().getId();

        MarkingStructure structure = resolveMarkingStructure(examTypeId, classId, subjectId, groupId);
        Class examClass = structure.getExamClass();

        Integer routineAcademicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, routineAcademicYearId, null, genderSectionId, sectionId, groupId, null, null);
        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

        List<StudentMark> marks = studentMarkRepository
                .findAllByEnrollmentIdInAndRoutineIdAndSubjectId(enrollmentIds, routineId, subjectId);

        Map<Long, BigDecimal> totalByEnrollment = new HashMap<>();
        Set<Long> appearedSet = new HashSet<>();
        for (StudentMark m : marks) {
            if (m.getMarksObtained() != null) {
                appearedSet.add(m.getEnrollmentId());
                totalByEnrollment.merge(m.getEnrollmentId(), m.getMarksObtained(), BigDecimal::add);
            }
        }

        List<Grade> sortedGrades = loadSortedGrades(examClass);
        StatsAggregator agg = new StatsAggregator();

        for (Long eid : enrollmentIds) {
            if (!appearedSet.contains(eid)) continue;
            BigDecimal total = totalByEnrollment.getOrDefault(eid, BigDecimal.ZERO);
            Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
            boolean subjectPassed = isSubjectPassed(total, structure.getPassMarks(), grade);
            agg.add(total.doubleValue(), grade != null ? grade.getName() : "F", subjectPassed);
        }

        SessionStatsResponse response = new SessionStatsResponse();
        response.setRoutineId(routineId);
        response.setSubjectId(subjectId);
        response.setClassName(examClass.getName());
        response.setSubjectName(structure.getSubject().getName());
        response.setExamTypeName(routine.getExamType().getName());
        response.setTotalEnrolled(enrollments.size());
        response.setAppeared(agg.appeared);
        response.setPassed(agg.passed);
        response.setFailed(agg.failed);
        response.setAverageMarks(agg.average());
        response.setHighestMarks(agg.highest());
        response.setLowestMarks(agg.lowest());
        response.setGradeDistribution(agg.gradeDistribution);
        return response;
    }

    // ─── PROGRESS REPORT DATA ────────────────────────────────────────────────────

    public ProgressReportData getProgressReportData(Integer examRoutineId, Integer classId, Integer shiftId,
                                                    Integer genderSectionId, Long sectionId, Integer groupId,
                                                    Integer startRoll, Integer endRoll) {
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));

        List<ExamSession> rawSessions = deduplicateBySubject(examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId));
        if (rawSessions.isEmpty()) throw new RuntimeException("No exam sessions found for this routine and class");

        // Curriculum filter: only subjects declared in class_subject_group for
        // this group (or as compulsory, group=NULL) make it through. Stops
        // exam_session rows from leaking subjects into the wrong group's
        // report card (e.g. AGRICULTURE tagged compulsory in the session but
        // declared in csg only for Humanities should not appear on a Science
        // student's card). The filter is conditional on the class actually
        // having a csg curriculum — if csg is empty we leave sessions alone
        // to preserve old behaviour.
        Set<Integer> curriculumSubjectIds = classSubjectGroupRepository
                .findByStudentClassIdAndIsActiveTrue(classId).stream()
                .filter(csg -> csg.getStudentGroup() == null
                        || (groupId != null && csg.getStudentGroup().getId().equals(groupId)))
                .map(csg -> csg.getSubject().getId())
                .collect(Collectors.toSet());
        if (!curriculumSubjectIds.isEmpty()) {
            rawSessions = rawSessions.stream()
                    .filter(s -> curriculumSubjectIds.contains(s.getSubject().getId()))
                    .collect(Collectors.toList());
            if (rawSessions.isEmpty()) {
                throw new RuntimeException("No exam sessions found for this routine and class");
            }
        }

        Class examClass = rawSessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> defaultFourthSubjectIds = loadFourthSubjectIds(classId, groupId);
        Map<Integer, Integer> mergeGroupMap = loadMergeGroupMap(classId, groupId);
        Map<Integer, Integer> mergeOrderMap = loadMergeOrderMap(classId, groupId);
        final List<ExamSession> sessions = sortSessionsByMergeOrder(rawSessions, mergeGroupMap, mergeOrderMap);

        Integer routineAcademicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> allEnrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, routineAcademicYearId, shiftId, null, null, groupId, null, null);
        SessionDataBundle bundle = loadSessionData(sessions, classId, allEnrollments);

        Map<Long, Integer> overrideMap = loadOverrideMap(allEnrollments);
        Map<Long, Set<Integer>> compulsoryMap = loadCompulsoryMap(allEnrollments);
        Set<Integer> includedFourthSubjectIds = buildIncludedFourthSubjectIds(overrideMap, compulsoryMap);

        // collect unique components ordered by orderIndex
        Map<Integer, ProgressReportData.ComponentInfo> componentMap = new LinkedHashMap<>();
        for (ExamSession s : sessions) {
            for (MarkingStructureComponent msc : bundle.sessionComponentsMap.getOrDefault(s.getId(), Collections.emptyList())) {
                ExamComponent ec = msc.getExamComponent();
                componentMap.computeIfAbsent(ec.getId(), id -> {
                    ProgressReportData.ComponentInfo ci = new ProgressReportData.ComponentInfo();
                    ci.setComponentId(ec.getId());
                    ci.setComponentName(ec.getName());
                    ci.setOrderIndex(ec.getOrderIndex() != null ? ec.getOrderIndex() : 999);
                    return ci;
                });
            }
        }
        List<ProgressReportData.ComponentInfo> components = componentMap.values().stream()
                .sorted(Comparator.comparingInt(ProgressReportData.ComponentInfo::getOrderIndex))
                .collect(Collectors.toList());

        // Ranks are computed after report-building (see below), so we can rank
        // by the same total the student sees on their marksheet: mandatory
        // marks plus 4th-subject marks when the student passed. Failed
        // students are dropped to 0 in the ranking map so they sort last and
        // the passed students get contiguous ranks 1..N_passed.

        // compute highest marks per session across all students
        Map<Integer, BigDecimal> highestBySession = new HashMap<>();
        for (ExamSession s : sessions) {
            if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
            List<MarkingStructureComponent> comps = bundle.sessionComponentsMap.get(s.getId());
            BigDecimal highest = BigDecimal.ZERO;
            for (Enrollment e : allEnrollments) {
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(e.getId(), Collections.emptyMap())
                        .getOrDefault(s.getSubject().getId(), Collections.emptyMap());
                if (!compMarks.isEmpty()) {
                    BigDecimal total = sumComponentMarks(comps, compMarks);
                    if (total.compareTo(highest) > 0) highest = total;
                }
            }
            highestBySession.put(s.getId(), highest);
        }

        // build subject infos
        List<ProgressReportData.SubjectInfo> subjectInfos = new ArrayList<>();
        for (ExamSession s : sessions) {
            if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
            if (defaultFourthSubjectIds.contains(s.getSubject().getId()) && !includedFourthSubjectIds.contains(s.getSubject().getId())) continue;
            MarkingStructure ms = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> comps = bundle.sessionComponentsMap.getOrDefault(s.getId(), Collections.emptyList());

            Map<Integer, Integer> compMaxMarks = new HashMap<>();
            for (MarkingStructureComponent msc : comps) {
                compMaxMarks.put(msc.getExamComponent().getId(), msc.getMaxMarks());
            }

            ProgressReportData.SubjectInfo si = new ProgressReportData.SubjectInfo();
            si.setSubjectId(s.getSubject().getId());
            si.setSubjectName(s.getSubject().getName());
            si.setFourthSubject(defaultFourthSubjectIds.contains(s.getSubject().getId()));
            si.setTotalMarks(ms.getTotalMarks());
            si.setHighestMarks(highestBySession.getOrDefault(s.getId(), BigDecimal.ZERO));
            si.setComponentMaxMarks(compMaxMarks);
            si.setMergeGroupId(mergeGroupMap.get(s.getSubject().getId()));
            subjectInfos.add(si);
        }

        // build student reports for ALL enrollments (needed for ranking);
        // display filter is applied at the end
        List<ProgressReportData.StudentReport> allReports = allEnrollments.stream().map(enrollment -> {
            Set<Integer> fourthSubjectIds = buildStudentFourthSet(enrollment.getId(), overrideMap, compulsoryMap);
            ProgressReportData.StudentReport report = new ProgressReportData.StudentReport();
            report.setEnrollmentId(enrollment.getId());
            report.setStudentSystemId(enrollment.getStudentSystemId());
            report.setClassRoll(enrollment.getClassRoll());

            if (enrollment.getStudent() != null) {
                report.setStudentName(enrollment.getStudent().getNameEnglish());
                report.setFatherName(enrollment.getStudent().getFatherNameEnglish());
                report.setMotherName(enrollment.getStudent().getMotherNameEnglish());
                report.setFatherPhone(enrollment.getStudent().getFatherPhone());
                report.setMotherPhone(enrollment.getStudent().getMotherPhone());
                if (enrollment.getStudent().getImage() != null)
                    report.setImageUrl(enrollment.getStudent().getImage().getImageUrl());
            }
            if (enrollment.getGenderSection() != null) report.setGenderSectionName(enrollment.getGenderSection().getGenderName());
            if (enrollment.getSection() != null) report.setSectionName(enrollment.getSection().getSectionName());
            if (enrollment.getStudentGroup() != null) report.setGroupName(enrollment.getStudentGroup().getGroupName());

            List<ProgressReportData.SubjectResult> subjectResults = new ArrayList<>();
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotal = BigDecimal.ZERO;
            BigDecimal fourthTotal = BigDecimal.ZERO;
            boolean overallPassed = true;
            int failedCount = 0;
            Map<Integer, BigDecimal> mgObtained = new HashMap<>();
            Map<Integer, Integer> mgTotalMax = new HashMap<>();
            Map<Integer, Integer> mgPassMarks = new HashMap<>();
            Map<Integer, Boolean> mgAnyAppeared = new HashMap<>();
            Map<Integer, Map<Integer, BigDecimal>> mgCompObtained = new HashMap<>();
            Map<Integer, Map<Integer, Integer>> mgCompPassMarks = new HashMap<>();

            for (ExamSession s : sessions) {
                if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
                if (defaultFourthSubjectIds.contains(s.getSubject().getId()) && !fourthSubjectIds.contains(s.getSubject().getId())) continue;
                if (!sessionAppliesToStudent(s, enrollment)) continue;
                MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
                List<MarkingStructureComponent> comps = bundle.sessionComponentsMap.get(s.getId());
                boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());
                Integer mergeGroupId = isFourth ? null : mergeGroupMap.get(s.getSubject().getId());

                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getSubject().getId(), Collections.emptyMap());
                boolean appeared = !compMarks.isEmpty();
                BigDecimal total = sumComponentMarks(comps, compMarks);

                // The (4th) badge reflects ONLY the student's actual 4th pick
                // (their override subject; falling back to the class default
                // when no override is set). Compulsory-junction subjects are
                // mandatory replacements for THIS student, not 4th picks,
                // so they must not get the badge.
                Integer studentOverrideSubject = overrideMap.get(enrollment.getId());
                boolean isStudentActualFourth = studentOverrideSubject != null
                        ? studentOverrideSubject.equals(s.getSubject().getId())
                        : defaultFourthSubjectIds.contains(s.getSubject().getId());

                ProgressReportData.SubjectResult sr = new ProgressReportData.SubjectResult();
                sr.setSubjectId(s.getSubject().getId());
                sr.setFourthSubject(isStudentActualFourth);
                sr.setAppeared(appeared);
                sr.setComponentMarks(new HashMap<>(compMarks));

                if (appeared) {
                    sr.setTotalMarks(total);
                    Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
                    boolean passed = isSubjectPassed(total, structure.getPassMarks(), grade, comps, compMarks);
                    if (!passed) grade = getFailGrade(sortedGrades);
                    if (grade != null) { sr.setGradeName(grade.getName()); sr.setGpaValue(grade.getGpaValue()); }
                    sr.setPassed(passed);
                    if (mergeGroupId != null) {
                        grandTotal = grandTotal.add(total);
                        mgObtained.merge(mergeGroupId, total, BigDecimal::add);
                        mgTotalMax.merge(mergeGroupId, structure.getTotalMarks(), Integer::sum);
                        if (structure.getPassMarks() != null) mgPassMarks.merge(mergeGroupId, structure.getPassMarks(), Integer::sum);
                        mgAnyAppeared.merge(mergeGroupId, true, Boolean::logicalOr);
                        for (MarkingStructureComponent comp : comps) {
                            if (comp.getPassMarks() != null) {
                                int cid = comp.getExamComponent().getId();
                                BigDecimal cm = compMarks.getOrDefault(cid, BigDecimal.ZERO);
                                mgCompObtained.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, cm, BigDecimal::add);
                                mgCompPassMarks.computeIfAbsent(mergeGroupId, k -> new HashMap<>()).merge(cid, comp.getPassMarks(), Integer::sum);
                            }
                        }
                    } else if (!isFourth) {
                        grandTotal = grandTotal.add(total);
                        if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                        if (!passed) { overallPassed = false; failedCount++; }
                    } else {
                        fourthTotal = fourthTotal.add(total);
                        if (grade != null && s.getSubject().getId().equals(overrideMap.get(enrollment.getId()))) {
                            fourthGpa = grade.getGpaValue();
                        }
                    }
                } else if (mergeGroupId != null) {
                    mgObtained.merge(mergeGroupId, BigDecimal.ZERO, BigDecimal::add);
                    mgTotalMax.merge(mergeGroupId, structure.getTotalMarks(), Integer::sum);
                    if (structure.getPassMarks() != null) mgPassMarks.merge(mergeGroupId, structure.getPassMarks(), Integer::sum);
                    mgAnyAppeared.merge(mergeGroupId, false, Boolean::logicalOr);
                } else if (!isFourth) {
                    overallPassed = false;
                    failedCount++;
                }
                subjectResults.add(sr);
            }

            int mergedFailed = applyMergeGroupGpas(mgObtained, mgTotalMax, mgPassMarks, mgCompObtained, mgCompPassMarks, mgAnyAppeared, sortedGrades, mandatoryGpas);
            if (mergedFailed > 0) { overallPassed = false; failedCount += mergedFailed; }

            report.setSubjectResults(subjectResults);
            // Passed students get the 4th subject folded into their total —
            // matches the marksheet footer and lines up the rank with what
            // the reader sees on the page.
            report.setTotalMarks(overallPassed ? grandTotal.add(fourthTotal) : grandTotal);
            report.setPassed(overallPassed);
            report.setFailedSubjectCount(failedCount);

            if (overallPassed && !mandatoryGpas.isEmpty()) {
                double sum = mandatoryGpas.stream().mapToDouble(Double::doubleValue).sum();
                report.setGpaWithout4th(round2(Math.min(5.0, sum / mandatoryGpas.size())));
                report.setOverallGpa(round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
            } else {
                report.setGpaWithout4th(0.0);
                report.setOverallGpa(0.0);
            }
            return report;
        }).collect(Collectors.toList());

        // Rank by the with-4th total for passed students; failed students
        // are pinned to 0 so they sort last and don't create rank gaps.
        Map<Long, BigDecimal> totalMarksMap = new HashMap<>();
        Map<Long, Double> gpaMap = new HashMap<>();
        for (ProgressReportData.StudentReport r : allReports) {
            totalMarksMap.put(r.getEnrollmentId(),
                    r.isPassed() ? r.getTotalMarks() : BigDecimal.ZERO);
            gpaMap.put(r.getEnrollmentId(),
                    r.isPassed() ? r.getOverallGpa() : 0.0);
        }
        RankMaps rankMaps = computeRankMaps(allEnrollments, totalMarksMap, gpaMap);
        for (ProgressReportData.StudentReport r : allReports) {
            if (r.isPassed()) {
                r.setClassRank(rankMaps.classRankMap.get(r.getEnrollmentId()));
                r.setGenderSectionRank(rankMaps.genderSectionRankMap.get(r.getEnrollmentId()));
                r.setSectionRank(rankMaps.sectionRankMap.get(r.getEnrollmentId()));
                r.setGroupRank(rankMaps.groupRankMap.get(r.getEnrollmentId()));
            } else {
                r.setClassRank(0);
                r.setGenderSectionRank(0);
                r.setSectionRank(0);
                r.setGroupRank(0);
            }
        }

        // Apply the display filter now that ranks are assigned
        Set<Long> filteredEnrollmentIds = allEnrollments.stream()
                .filter(e -> matchesFilter(e, genderSectionId, sectionId, groupId, startRoll, endRoll))
                .map(Enrollment::getId)
                .collect(Collectors.toSet());
        List<ProgressReportData.StudentReport> studentReports = allReports.stream()
                .filter(r -> filteredEnrollmentIds.contains(r.getEnrollmentId()))
                .collect(Collectors.toList());

        studentReports.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));

        ProgressReportData result = new ProgressReportData();
        result.setExamRoutineId(examRoutineId);
        result.setRoutineTitle(routine.getTitle());
        result.setExamTypeName(routine.getExamType().getName());
        result.setClassName(examClass.getName());
        result.setAcademicYearName(routine.getAcademicYear() != null ? routine.getAcademicYear().getYearName() : "");
        result.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        result.setComponents(components);
        result.setSubjects(subjectInfos);
        result.setStudents(studentReports);
        return result;
    }

    /**
     * Progress-report partition. Returns the data already split per group so
     * the PDF service can render Science students with Science subjects, then
     * Humanities students with Humanities subjects, etc. — no shared subject
     * list with blanks across groups.
     *
     * - When a groupId is passed, returns a single-element list containing
     *   the result for that group (same as a direct getProgressReportData
     *   call).
     * - When groupId is null, partitions the class by student_group_id and
     *   calls getProgressReportData once per group, plus once for any
     *   no-group bucket. Each ProgressReportData carries the curriculum-
     *   filtered subjects list relevant to its group.
     *
     * Used only by ProgressReportPdfService — other consumers stick with
     * getProgressReportData.
     */
    public List<ProgressReportData> getProgressReportDataPartitioned(
            Integer examRoutineId, Integer classId, Integer shiftId,
            Integer genderSectionId, Long sectionId, Integer groupId,
            Integer startRoll, Integer endRoll) {

        if (groupId != null) {
            return List.of(getProgressReportData(examRoutineId, classId, shiftId,
                    genderSectionId, sectionId, groupId, startRoll, endRoll));
        }

        // Discover the distinct groups present in the class. We need both the
        // group ids AND whether there's a no-group bucket. Order preserved
        // so the PDF renders groups consistently across downloads.
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));
        Integer academicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> allEnrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, academicYearId, shiftId, null, null, null, null, null);

        LinkedHashSet<Integer> distinctGroups = new LinkedHashSet<>();
        boolean hasNoGroupBucket = false;
        for (Enrollment e : allEnrollments) {
            if (e.getStudentGroup() != null) distinctGroups.add(e.getStudentGroup().getId());
            else hasNoGroupBucket = true;
        }

        List<ProgressReportData> partitions = new ArrayList<>();
        for (Integer gId : distinctGroups) {
            try {
                partitions.add(getProgressReportData(examRoutineId, classId, shiftId,
                        genderSectionId, sectionId, gId, startRoll, endRoll));
            } catch (RuntimeException ignored) {
                // No sessions for this group — skip rather than fail the
                // whole PDF.
            }
        }
        if (hasNoGroupBucket) {
            try {
                ProgressReportData data = getProgressReportData(examRoutineId, classId, shiftId,
                        genderSectionId, sectionId, null, startRoll, endRoll);
                // When called with groupId=null on the no-group bucket, the
                // method returns all students. We need to narrow the students
                // list to only those without a group assigned.
                List<ProgressReportData.StudentReport> rows = data.getStudents().stream()
                        .filter(r -> r.getGroupName() == null || r.getGroupName().isBlank())
                        .collect(Collectors.toList());
                data.setStudents(rows);
                partitions.add(data);
            } catch (RuntimeException ignored) {}
        }
        return partitions;
    }

    // ─── STATS – ROUTINE ─────────────────────────────────────────────────────────

    public RoutineStatsResponse getRoutineStats(Integer examRoutineId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));

        List<ExamSession> sessions = deduplicateBySubject(examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId));
        if (sessions.isEmpty()) throw new RuntimeException("No sessions found");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        Integer routineAcademicYearId = routine.getAcademicYear() != null ? routine.getAcademicYear().getId() : null;
        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, routineAcademicYearId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        SessionDataBundle bundle = loadSessionData(sessions, classId, enrollments);

        List<RoutineStatsResponse.SubjectStats> subjectStatsList = new ArrayList<>();
        for (ExamSession s : sessions) {
            if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
            MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
            StatsAggregator agg = new StatsAggregator();

            for (Enrollment enrollment : enrollments) {
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getSubject().getId(), Collections.emptyMap());
                if (compMarks.isEmpty()) continue;
                BigDecimal total = sumComponentMarks(components, compMarks);
                Grade grade = resolveGradeByPercentage(total, structure.getTotalMarks(), sortedGrades);
                agg.add(total.doubleValue(), grade != null ? grade.getName() : "F",
                        isSubjectPassed(total, structure.getPassMarks(), grade));
            }

            RoutineStatsResponse.SubjectStats stats = new RoutineStatsResponse.SubjectStats();
            stats.setSubjectId(s.getSubject().getId());
            stats.setSubjectName(s.getSubject().getName());
            stats.setFourthSubject(fourthSubjectIds.contains(s.getSubject().getId()));
            stats.setAppeared(agg.appeared);
            stats.setPassed(agg.passed);
            stats.setFailed(agg.failed);
            stats.setAverageMarks(agg.average());
            stats.setHighestMarks(agg.highest());
            stats.setLowestMarks(agg.lowest());
            stats.setGradeDistribution(agg.gradeDistribution);
            subjectStatsList.add(stats);
        }

        RoutineStatsResponse response = new RoutineStatsResponse();
        response.setExamRoutineId(examRoutineId);
        response.setRoutineTitle(routine.getTitle());
        response.setExamTypeName(routine.getExamType().getName());
        response.setClassName(examClass.getName());
        response.setTotalEnrolled(enrollments.size());
        response.setSubjectStats(subjectStatsList);
        return response;
    }

    // ─── STATS – ANNUAL ──────────────────────────────────────────────────────────

    public AnnualStatsResponse getAnnualStats(Integer academicYearId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new RuntimeException("Academic year not found: " + academicYearId));

        List<ExamSession> sessions = deduplicateByRoutineAndSubject(examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId));
        if (sessions.isEmpty()) throw new RuntimeException("No sessions found for this academic year");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, academicYearId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        AnnualDataBundle bundle = loadAnnualData(sessions, classId, enrollments);
        List<Integer> orderedSubjectIds = bundle.sessionsBySubject.keySet().stream().sorted().collect(Collectors.toList());

        Map<Integer, String> subjectNameMap = sessions.stream()
                .collect(Collectors.toMap(s -> s.getSubject().getId(), s -> s.getSubject().getName(), (a, b) -> a));

        List<AnnualStatsResponse.SubjectStats> subjectStatsList = new ArrayList<>();
        for (Integer subjectId : orderedSubjectIds) {
            List<ExamSession> subjectSessions = bundle.sessionsBySubject.get(subjectId).stream()
                    .filter(s -> bundle.sessionStructureMap.containsKey(s.getId()))
                    .collect(Collectors.toList());
            if (subjectSessions.isEmpty()) continue;

            MarkingStructure refStructure = bundle.sessionStructureMap.get(subjectSessions.get(0).getId());
            StatsAggregator agg = new StatsAggregator();

            for (Enrollment enrollment : enrollments) {
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null || !asd.appeared || asd.totalMax == 0) continue;
                agg.add(asd.scaled.doubleValue(), asd.grade != null ? asd.grade.getName() : "F", asd.passed);
            }

            AnnualStatsResponse.SubjectStats stats = new AnnualStatsResponse.SubjectStats();
            stats.setSubjectId(subjectId);
            stats.setSubjectName(subjectNameMap.getOrDefault(subjectId, ""));
            stats.setFourthSubject(fourthSubjectIds.contains(subjectId));
            stats.setAppeared(agg.appeared);
            stats.setPassed(agg.passed);
            stats.setFailed(agg.failed);
            stats.setAverageMarks(agg.average());
            stats.setHighestMarks(agg.highest());
            stats.setLowestMarks(agg.lowest());
            stats.setGradeDistribution(agg.gradeDistribution);
            subjectStatsList.add(stats);
        }

        AnnualStatsResponse response = new AnnualStatsResponse();
        response.setAcademicYearId(academicYearId);
        response.setAcademicYearName(year.getYearName());
        response.setClassName(examClass.getName());
        response.setTotalEnrolled(enrollments.size());
        response.setSubjectStats(subjectStatsList);
        return response;
    }

    // ─── SESSION DEDUPLICATION ───────────────────────────────────────────────────

    private List<ExamSession> deduplicateBySubject(List<ExamSession> sessions) {
        Map<Integer, ExamSession> seen = new LinkedHashMap<>();
        for (ExamSession s : sessions) {
            seen.putIfAbsent(s.getSubject().getId(), s);
        }
        return new ArrayList<>(seen.values());
    }

    private List<ExamSession> deduplicateByRoutineAndSubject(List<ExamSession> sessions) {
        Map<String, ExamSession> seen = new LinkedHashMap<>();
        for (ExamSession s : sessions) {
            seen.putIfAbsent(s.getExamRoutine().getId() + "_" + s.getSubject().getId(), s);
        }
        return new ArrayList<>(seen.values());
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    // Grade policy ranges are 0-100 percentages, so raw marks must be scaled first.
    private Grade resolveGradeByPercentage(BigDecimal obtained, int totalMax, List<Grade> sortedGradesDesc) {
        if (totalMax <= 0) return resolveGrade(0, sortedGradesDesc);
        double pct = obtained.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalMax), 4, RoundingMode.HALF_UP)
                .doubleValue();
        return resolveGrade(pct, sortedGradesDesc);
    }

    private Grade resolveGrade(double marks, List<Grade> sortedGradesDesc) {
        if (sortedGradesDesc.isEmpty()) return null;
        return sortedGradesDesc.stream()
                .filter(g -> marks >= g.getMinMark())
                .findFirst()
                .orElse(sortedGradesDesc.get(sortedGradesDesc.size() - 1));
    }

    private double computeOverallGpa(List<Double> mandatoryGpas, Double fourthSubjectGpa) {
        double sum = mandatoryGpas.stream().mapToDouble(Double::doubleValue).sum();
        double bonus = fourthSubjectBonus(fourthSubjectGpa);
        return Math.min(5.0, (sum + bonus) / mandatoryGpas.size());
    }

    private double fourthSubjectBonus(Double gpaValue) {
        if (gpaValue == null) return 0.0;
        if (gpaValue >= 5.0) return 3.0;
        if (gpaValue >= 4.0) return 2.0;
        if (gpaValue >= 3.5) return 1.0;
        return 0.0;
    }

    private boolean isSubjectPassed(BigDecimal total, Integer passMarks, Grade grade) {
        if (passMarks != null) return total.compareTo(BigDecimal.valueOf(passMarks)) >= 0;
        return grade != null && grade.getGpaValue() > 0.0;
    }

    private boolean anyComponentFailed(List<MarkingStructureComponent> components, Map<Integer, BigDecimal> compMarks) {
        return components.stream()
                .filter(c -> c.getPassMarks() != null)
                .anyMatch(c -> compMarks.getOrDefault(c.getExamComponent().getId(), BigDecimal.ZERO)
                        .compareTo(BigDecimal.valueOf(c.getPassMarks())) < 0);
    }

    private Grade getFailGrade(List<Grade> sortedGrades) {
        return sortedGrades.isEmpty() ? null : sortedGrades.get(sortedGrades.size() - 1);
    }

    private boolean isSubjectPassed(BigDecimal total, Integer passMarks, Grade grade,
                                    List<MarkingStructureComponent> components, Map<Integer, BigDecimal> compMarks) {
        if (anyComponentFailed(components, compMarks)) return false;
        if (passMarks != null) return total.compareTo(BigDecimal.valueOf(passMarks)) >= 0;
        return grade != null && grade.getGpaValue() > 0.0;
    }

    private List<Grade> loadSortedGrades(Class examClass) {
        if (examClass.getGradingPolicy() == null) return Collections.emptyList();
        List<Grade> grades = gradeRepository.findByGradingPolicyId(examClass.getGradingPolicy().getId());
        grades.sort(Comparator.comparingDouble(Grade::getMinMark).reversed());
        return grades;
    }

    // Batch-load override map for a list of enrollments: enrollmentId -> overridden subjectId (GPA bonus subject)
    private Map<Long, Integer> loadOverrideMap(List<Enrollment> enrollments) {
        List<Long> ids = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
        Map<Long, Integer> map = new HashMap<>();
        for (StudentFourthSubjectOverride o : studentFourthSubjectOverrideRepository.findByEnrollmentIdIn(ids)) {
            if (o.getSubject() != null) {
                map.put(o.getEnrollmentId(), o.getSubject().getId());
            }
        }
        return map;
    }

    private Map<Long, Set<Integer>> loadCompulsoryMap(List<Enrollment> enrollments) {
        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
        List<StudentFourthSubjectOverride> overrides = studentFourthSubjectOverrideRepository.findByEnrollmentIdIn(enrollmentIds);
        Map<Long, Set<Integer>> map = new HashMap<>();
        for (StudentFourthSubjectOverride o : overrides) {
            if (o.getCompulsorySubjects() != null && !o.getCompulsorySubjects().isEmpty()) {
                map.put(o.getEnrollmentId(), o.getCompulsorySubjects().stream()
                        .map(s -> s.getId())
                        .collect(Collectors.toSet()));
            }
        }
        return map;
    }

    private Set<Integer> buildStudentFourthSet(Long enrollmentId,
                                                Map<Long, Integer> overrideMap,
                                                Map<Long, Set<Integer>> compulsoryMap) {
        Set<Integer> result = new HashSet<>();
        Integer override = overrideMap.get(enrollmentId);
        if (override != null) result.add(override);
        Set<Integer> compulsory = compulsoryMap.get(enrollmentId);
        if (compulsory != null) result.addAll(compulsory);
        return result;
    }

    private Set<Integer> buildIncludedFourthSubjectIds(Map<Long, Integer> overrideMap,
                                                        Map<Long, Set<Integer>> compulsoryMap) {
        Set<Integer> result = new HashSet<>(overrideMap.values());
        for (Set<Integer> s : compulsoryMap.values()) result.addAll(s);
        return result;
    }

    private Set<Integer> loadFourthSubjectIds(Integer classId, Integer groupId) {
        return classSubjectGroupRepository.findByStudentClassIdAndIsActiveTrue(classId).stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsFourthSubject()))
                .map(g -> g.getSubject().getId())
                .collect(Collectors.toSet());
    }

    private Map<Integer, Integer> loadMergeGroupMap(Integer classId, Integer groupId) {
        List<ClassSubjectGroup> groups = groupId != null
                ? classSubjectGroupRepository.findSubjectsForStudent(classId, groupId)
                : classSubjectGroupRepository.findByStudentClassIdAndIsActiveTrue(classId);
        return groups.stream()
                .filter(g -> g.getMergeGroupId() != null)
                .collect(Collectors.toMap(
                        g -> g.getSubject().getId(),
                        ClassSubjectGroup::getMergeGroupId,
                        (a, b) -> a));
    }

    private Map<Integer, Integer> loadMergeOrderMap(Integer classId, Integer groupId) {
        List<ClassSubjectGroup> groups = groupId != null
                ? classSubjectGroupRepository.findSubjectsForStudent(classId, groupId)
                : classSubjectGroupRepository.findByStudentClassIdAndIsActiveTrue(classId);
        return groups.stream()
                .collect(Collectors.toMap(
                        g -> g.getSubject().getId(),
                        g -> g.getMergeOrderIndex() != null ? g.getMergeOrderIndex() : 0,
                        (a, b) -> a));
    }

    private List<ExamSession> sortSessionsByMergeOrder(List<ExamSession> sessions,
                                                        Map<Integer, Integer> mergeGroupMap,
                                                        Map<Integer, Integer> mergeOrderMap) {
        Map<Integer, Integer> originalIdx = new HashMap<>();
        Map<Integer, Integer> mgFirstPos = new HashMap<>();
        for (int i = 0; i < sessions.size(); i++) {
            ExamSession s = sessions.get(i);
            originalIdx.put(s.getId(), i);
            Integer mgId = mergeGroupMap.get(s.getSubject().getId());
            if (mgId != null) mgFirstPos.putIfAbsent(mgId, i);
        }
        List<ExamSession> sorted = new ArrayList<>(sessions);
        sorted.sort(Comparator.comparingInt(s -> {
            Integer mgId = mergeGroupMap.get(s.getSubject().getId());
            if (mgId == null) return originalIdx.getOrDefault(s.getId(), 0) * 1000;
            return mgFirstPos.getOrDefault(mgId, 0) * 1000 + mergeOrderMap.getOrDefault(s.getSubject().getId(), 0);
        }));
        return sorted;
    }

    private List<Integer> sortSubjectIdsByMergeOrder(List<Integer> subjectIds,
                                                      Map<Integer, Integer> mergeGroupMap,
                                                      Map<Integer, Integer> mergeOrderMap) {
        Map<Integer, Integer> originalIdx = new HashMap<>();
        Map<Integer, Integer> mgFirstPos = new HashMap<>();
        for (int i = 0; i < subjectIds.size(); i++) {
            Integer sid = subjectIds.get(i);
            originalIdx.put(sid, i);
            Integer mgId = mergeGroupMap.get(sid);
            if (mgId != null) mgFirstPos.putIfAbsent(mgId, i);
        }
        List<Integer> sorted = new ArrayList<>(subjectIds);
        sorted.sort(Comparator.comparingInt(sid -> {
            Integer mgId = mergeGroupMap.get(sid);
            if (mgId == null) return originalIdx.getOrDefault(sid, 0) * 1000;
            return mgFirstPos.getOrDefault(mgId, 0) * 1000 + mergeOrderMap.getOrDefault(sid, 0);
        }));
        return sorted;
    }

    // Returns combined pass/fail per merge group ID, using the same accumulated maps.
    private Map<Integer, Boolean> buildMergePassMap(
            Map<Integer, BigDecimal> mgObtained,
            Map<Integer, Integer> mgTotalMax,
            Map<Integer, Integer> mgPassMarks,
            Map<Integer, Map<Integer, BigDecimal>> mgCompObtained,
            Map<Integer, Map<Integer, Integer>> mgCompPassMarks,
            Map<Integer, Boolean> mgAnyAppeared,
            List<Grade> sortedGrades) {
        Map<Integer, Boolean> result = new HashMap<>();
        for (Integer gid : mgObtained.keySet()) {
            if (!mgAnyAppeared.getOrDefault(gid, false)) { result.put(gid, false); continue; }
            Map<Integer, Integer> compPass = mgCompPassMarks.getOrDefault(gid, Collections.emptyMap());
            Map<Integer, BigDecimal> compObt = mgCompObtained.getOrDefault(gid, Collections.emptyMap());
            boolean anyCompFail = compPass.entrySet().stream()
                    .anyMatch(e -> compObt.getOrDefault(e.getKey(), BigDecimal.ZERO)
                            .compareTo(BigDecimal.valueOf(e.getValue())) < 0);
            if (anyCompFail) { result.put(gid, false); continue; }
            BigDecimal combined = mgObtained.get(gid);
            int combinedMax = mgTotalMax.getOrDefault(gid, 0);
            if (combinedMax == 0) continue;
            Integer combinedPass = mgPassMarks.isEmpty() ? null : mgPassMarks.get(gid);
            Grade grade = resolveGradeByPercentage(combined, combinedMax, sortedGrades);
            result.put(gid, isSubjectPassed(combined, combinedPass, grade));
        }
        return result;
    }

    // Processes accumulated merge group marks and appends merged GPAs to mandatoryGpas.
    // Returns the number of failed (or not-appeared) merge groups.
    private int applyMergeGroupGpas(
            Map<Integer, BigDecimal> mgObtained,
            Map<Integer, Integer> mgTotalMax,
            Map<Integer, Integer> mgPassMarks,
            Map<Integer, Map<Integer, BigDecimal>> mgCompObtained,
            Map<Integer, Map<Integer, Integer>> mgCompPassMarks,
            Map<Integer, Boolean> mgAnyAppeared,
            List<Grade> sortedGrades,
            List<Double> mandatoryGpas) {
        int failed = 0;
        for (Integer gid : mgObtained.keySet()) {
            if (!mgAnyAppeared.getOrDefault(gid, false)) { failed++; continue; }
            Map<Integer, Integer> compPass = mgCompPassMarks.getOrDefault(gid, Collections.emptyMap());
            Map<Integer, BigDecimal> compObt = mgCompObtained.getOrDefault(gid, Collections.emptyMap());
            boolean anyCompFail = compPass.entrySet().stream()
                    .anyMatch(e -> compObt.getOrDefault(e.getKey(), BigDecimal.ZERO)
                            .compareTo(BigDecimal.valueOf(e.getValue())) < 0);
            if (anyCompFail) { failed++; continue; }
            BigDecimal combined = mgObtained.get(gid);
            int combinedMax = mgTotalMax.getOrDefault(gid, 0);
            if (combinedMax == 0) continue;
            Integer combinedPass = mgPassMarks.isEmpty() ? null : mgPassMarks.get(gid);
            Grade grade = resolveGradeByPercentage(combined, combinedMax, sortedGrades);
            if (grade != null) mandatoryGpas.add(grade.getGpaValue());
            if (!isSubjectPassed(combined, combinedPass, grade)) failed++;
        }
        return failed;
    }

    private MarkingStructure resolveMarkingStructure(Integer examTypeId, Integer classId, Integer subjectId, Integer groupId) {
        List<MarkingStructure> structures = groupId != null
                ? markingStructureRepository.findAllByGroupIdAndFilters(examTypeId, classId, subjectId, groupId)
                : markingStructureRepository.findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId);
        if (structures.isEmpty() && groupId != null) {
            structures = markingStructureRepository.findClassWideAndDeletedAtIsNull(examTypeId, classId, subjectId);
        }
        if (structures.isEmpty()) throw new RuntimeException(
                "No marking structure found for examType=" + examTypeId + " class=" + classId + " subject=" + subjectId);
        return structures.get(0);
    }

    private BigDecimal sumComponentMarks(List<MarkingStructureComponent> components, Map<Integer, BigDecimal> compMarks) {
        return components.stream()
                .filter(c -> compMarks.containsKey(c.getExamComponent().getId()))
                .map(c -> {
                    BigDecimal val = compMarks.get(c.getExamComponent().getId());
                    return val != null ? val : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ─── RANK & MERIT HELPERS ────────────────────────────────────────────────────

    private static class RankMaps {
        Map<Long, Integer> classRankMap = new HashMap<>();
        Map<Long, Integer> genderSectionRankMap = new HashMap<>();
        Map<Long, Integer> sectionRankMap = new HashMap<>();
        Map<Long, Integer> groupRankMap = new HashMap<>();
    }

    private RankMaps computeRankMaps(List<Enrollment> allEnrollments, Map<Long, BigDecimal> totalMarksMap, Map<Long, Double> gpaMap) {
        RankMaps rm = new RankMaps();
        // Tie-break: higher GPA wins; then lower class roll wins (nulls last);
        // then lower enrollment id — so ordering is fully deterministic.
        Comparator<Enrollment> byMarksDesc = Comparator
                .comparing((Enrollment e) -> totalMarksMap.getOrDefault(e.getId(), BigDecimal.ZERO), Comparator.reverseOrder())
                .thenComparing(e -> gpaMap.getOrDefault(e.getId(), 0.0), Comparator.reverseOrder())
                .thenComparing(e -> e.getClassRoll() != null ? e.getClassRoll() : Integer.MAX_VALUE)
                .thenComparing(Enrollment::getId);

        // class rank — all students
        List<Enrollment> sortedAll = allEnrollments.stream().sorted(byMarksDesc).collect(Collectors.toList());
        for (int i = 0; i < sortedAll.size(); i++) rm.classRankMap.put(sortedAll.get(i).getId(), i + 1);

        // gender section rank
        allEnrollments.stream()
                .filter(e -> e.getGenderSection() != null)
                .collect(Collectors.groupingBy(e -> e.getGenderSection().getId()))
                .forEach((gsId, group) -> {
                    List<Enrollment> sorted = group.stream().sorted(byMarksDesc).collect(Collectors.toList());
                    for (int i = 0; i < sorted.size(); i++) rm.genderSectionRankMap.put(sorted.get(i).getId(), i + 1);
                });

        // section rank — use section if set, fall back to genderSection for students without a section
        allEnrollments.stream()
                .filter(e -> e.getSection() != null)
                .collect(Collectors.groupingBy(e -> e.getSection().getId()))
                .forEach((secId, group) -> {
                    List<Enrollment> sorted = group.stream().sorted(byMarksDesc).collect(Collectors.toList());
                    for (int i = 0; i < sorted.size(); i++) rm.sectionRankMap.put(sorted.get(i).getId(), i + 1);
                });

        allEnrollments.stream()
                .filter(e -> e.getSection() == null && e.getGenderSection() != null)
                .collect(Collectors.groupingBy(e -> e.getGenderSection().getId()))
                .forEach((gsId, group) -> {
                    List<Enrollment> sorted = group.stream().sorted(byMarksDesc).collect(Collectors.toList());
                    for (int i = 0; i < sorted.size(); i++) rm.sectionRankMap.put(sorted.get(i).getId(), i + 1);
                });

        // group rank
        allEnrollments.stream()
                .filter(e -> e.getStudentGroup() != null)
                .collect(Collectors.groupingBy(e -> e.getStudentGroup().getId()))
                .forEach((grpId, group) -> {
                    List<Enrollment> sorted = group.stream().sorted(byMarksDesc).collect(Collectors.toList());
                    for (int i = 0; i < sorted.size(); i++) rm.groupRankMap.put(sorted.get(i).getId(), i + 1);
                });

        return rm;
    }

    private boolean matchesFilter(Enrollment e, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        if (genderSectionId != null && (e.getGenderSection() == null || !e.getGenderSection().getId().equals(genderSectionId))) return false;
        if (sectionId != null && (e.getSection() == null || !e.getSection().getId().equals(sectionId))) return false;
        if (groupId != null && (e.getStudentGroup() == null || !e.getStudentGroup().getId().equals(groupId))) return false;
        if (startRoll != null && (e.getClassRoll() == null || e.getClassRoll() < startRoll)) return false;
        if (endRoll != null && (e.getClassRoll() == null || e.getClassRoll() > endRoll)) return false;
        return true;
    }

    /**
     * True if a session applies to the given student.
     *   - Compulsory session (session.group_id == null)  → applies to everyone.
     *   - Group-tagged session (session.group_id == G)   → applies only to
     *     students in group G.
     *
     * Used to suppress false "did not appear" failures when the API is called
     * without a group filter. Without this check, sessions from every group
     * are loaded into the list and the per-student loop marks a Science
     * student as failed for a Humanities subject they were never expected
     * to sit for — which drags their overall GPA to 0 and the grade to F.
     */
    private boolean sessionAppliesToStudent(ExamSession s, Enrollment enrollment) {
        Integer sessionGroupId = s.getGroup() != null ? s.getGroup().getId() : null;
        if (sessionGroupId == null) return true;
        Integer studentGroupId = enrollment.getStudentGroup() != null ? enrollment.getStudentGroup().getId() : null;
        return sessionGroupId.equals(studentGroupId);
    }

    /**
     * Subject-level counterpart for the annual / overview / stats loops that
     * iterate subjectIds rather than sessions. True if at least one session
     * for the subject applies to the student. False when every session for
     * the subject is tagged for a different group — i.e. the subject isn't
     * part of this student's curriculum.
     */
    private boolean subjectAppliesToStudent(Integer subjectId,
                                              Map<Integer, List<ExamSession>> sessionsBySubject,
                                              Enrollment enrollment) {
        List<ExamSession> sessions = sessionsBySubject.get(subjectId);
        if (sessions == null || sessions.isEmpty()) return true;
        for (ExamSession s : sessions) {
            if (sessionAppliesToStudent(s, enrollment)) return true;
        }
        return false;
    }

    // ─── DATA BUNDLE HELPERS ─────────────────────────────────────────────────────

    private static class SessionDataBundle {
        List<Enrollment> enrollments;
        Map<Integer, MarkingStructure> sessionStructureMap = new HashMap<>();
        Map<Integer, List<MarkingStructureComponent>> sessionComponentsMap = new HashMap<>();
        // enrollmentId -> subjectId -> componentId -> marks
        Map<Long, Map<Integer, Map<Integer, BigDecimal>>> markMap = new HashMap<>();
    }

    private SessionDataBundle loadSessionData(List<ExamSession> sessions, Integer classId, List<Enrollment> enrollments) {
        SessionDataBundle bundle = new SessionDataBundle();
        bundle.enrollments = enrollments;

        for (ExamSession s : sessions) {
            Integer examTypeId = s.getExamRoutine().getExamType().getId();
            Integer resolvedGroupId = s.getGroup() != null ? s.getGroup().getId() : null;
            try {
                MarkingStructure structure = resolveMarkingStructure(examTypeId, classId, s.getSubject().getId(), resolvedGroupId);
                bundle.sessionStructureMap.put(s.getId(), structure);
            } catch (RuntimeException ignored) {}
        }

        if (!bundle.sessionStructureMap.isEmpty()) {
            List<MarkingStructure> structures = new ArrayList<>(bundle.sessionStructureMap.values());
            List<MarkingStructureComponent> allComponents = markingStructureComponentRepository
                    .findAllByMarkingStructureInAndDeletedAtIsNull(structures);
            Map<Integer, List<MarkingStructureComponent>> byStructureId = allComponents.stream()
                    .collect(Collectors.groupingBy(c -> c.getMarkingStructure().getId()));
            bundle.sessionStructureMap.forEach((sessionId, structure) ->
                    bundle.sessionComponentsMap.put(sessionId, byStructureId.getOrDefault(structure.getId(), Collections.emptyList())));
        }

        if (!enrollments.isEmpty() && !sessions.isEmpty()) {
            List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
            Integer routineId = sessions.get(0).getExamRoutine().getId();
            List<StudentMark> allMarks = studentMarkRepository
                    .findAllByEnrollmentIdsAndRoutineId(enrollmentIds, routineId);
            for (StudentMark m : allMarks) {
                bundle.markMap.computeIfAbsent(m.getEnrollmentId(), k -> new HashMap<>())
                        .computeIfAbsent(m.getSubjectId(), k -> new HashMap<>())
                        .put(m.getExamComponent().getId(), m.getMarksObtained());
            }
        }
        return bundle;
    }

    private static class AnnualDataBundle {
        Map<Integer, List<ExamSession>> sessionsBySubject;
        Map<Integer, MarkingStructure> sessionStructureMap = new HashMap<>();
        Map<Integer, List<MarkingStructureComponent>> sessionComponentsMap = new HashMap<>();
        // enrollmentId -> "routineId_subjectId" -> componentId -> marks
        Map<Long, Map<String, Map<Integer, BigDecimal>>> markMap = new HashMap<>();
    }

    private AnnualDataBundle loadAnnualData(List<ExamSession> sessions, Integer classId, List<Enrollment> enrollments) {
        AnnualDataBundle bundle = new AnnualDataBundle();
        bundle.sessionsBySubject = sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getSubject().getId()));

        for (ExamSession s : sessions) {
            Integer examTypeId = s.getExamRoutine().getExamType().getId();
            Integer resolvedGroupId = s.getGroup() != null ? s.getGroup().getId() : null;
            try {
                bundle.sessionStructureMap.put(s.getId(),
                        resolveMarkingStructure(examTypeId, classId, s.getSubject().getId(), resolvedGroupId));
            } catch (RuntimeException ignored) {}
        }

        if (!bundle.sessionStructureMap.isEmpty()) {
            List<MarkingStructure> structures = new ArrayList<>(bundle.sessionStructureMap.values());
            List<MarkingStructureComponent> allComponents = markingStructureComponentRepository
                    .findAllByMarkingStructureInAndDeletedAtIsNull(structures);
            Map<Integer, List<MarkingStructureComponent>> byStructureId = allComponents.stream()
                    .collect(Collectors.groupingBy(c -> c.getMarkingStructure().getId()));
            bundle.sessionStructureMap.forEach((sessionId, structure) ->
                    bundle.sessionComponentsMap.put(sessionId, byStructureId.getOrDefault(structure.getId(), Collections.emptyList())));
        }

        if (!enrollments.isEmpty() && !sessions.isEmpty()) {
            List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
            List<Integer> routineIds = sessions.stream()
                    .map(s -> s.getExamRoutine().getId())
                    .distinct()
                    .collect(Collectors.toList());
            List<StudentMark> allMarks = studentMarkRepository
                    .findAllByEnrollmentIdsAndRoutineIds(enrollmentIds, routineIds);
            for (StudentMark m : allMarks) {
                String key = m.getRoutineId() + "_" + m.getSubjectId();
                bundle.markMap.computeIfAbsent(m.getEnrollmentId(), k -> new HashMap<>())
                        .computeIfAbsent(key, k -> new HashMap<>())
                        .put(m.getExamComponent().getId(), m.getMarksObtained());
            }
        }
        return bundle;
    }

    private static class AnnualSubjectData {
        boolean appeared;
        BigDecimal totalObtained;
        int totalMax;
        BigDecimal scaled;
        Grade grade;
        boolean passed;
        List<RoutineBreakdownData> routineBreakdowns = new ArrayList<>();

        static class RoutineBreakdownData {
            Integer routineId;
            BigDecimal marksObtained;
            int maxMarks;
            Grade grade;
            boolean passed;
            boolean appeared;
        }
    }

    private AnnualSubjectData computeAnnualSubjectData(Integer subjectId, AnnualDataBundle bundle, Long enrollmentId, List<Grade> sortedGrades) {
        List<ExamSession> subjectSessions = bundle.sessionsBySubject.getOrDefault(subjectId, Collections.emptyList())
                .stream().filter(s -> bundle.sessionStructureMap.containsKey(s.getId())).collect(Collectors.toList());
        if (subjectSessions.isEmpty()) return null;

        boolean appeared = false;
        BigDecimal totalObtained = BigDecimal.ZERO;
        int totalMax = 0;
        boolean anyRoutineFailed = false;
        List<AnnualSubjectData.RoutineBreakdownData> routineBreakdowns = new ArrayList<>();

        for (ExamSession s : subjectSessions) {
            MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> components = bundle.sessionComponentsMap.getOrDefault(s.getId(), Collections.emptyList());
            String markKey = s.getExamRoutine().getId() + "_" + s.getSubject().getId();
            Map<Integer, BigDecimal> compMarks = bundle.markMap
                    .getOrDefault(enrollmentId, Collections.emptyMap())
                    .getOrDefault(markKey, Collections.emptyMap());

            boolean routineAppeared = !compMarks.isEmpty();
            BigDecimal routineObtained = sumComponentMarks(components, compMarks);
            int routineMax = structure.getTotalMarks();

            if (routineAppeared) appeared = true;
            totalObtained = totalObtained.add(routineObtained);
            totalMax += routineMax;

            AnnualSubjectData.RoutineBreakdownData rbd = new AnnualSubjectData.RoutineBreakdownData();
            rbd.routineId = s.getExamRoutine().getId();
            rbd.marksObtained = routineObtained;
            rbd.maxMarks = routineMax;
            rbd.appeared = routineAppeared;

            if (routineAppeared && routineMax > 0) {
                BigDecimal routineScaled = routineObtained.multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(routineMax), 2, RoundingMode.HALF_UP);
                rbd.grade = resolveGrade(routineScaled.doubleValue(), sortedGrades);
                rbd.passed = isSubjectPassed(routineScaled, structure.getPassMarks(), rbd.grade, components, compMarks);
                if (!rbd.passed) rbd.grade = getFailGrade(sortedGrades);
                if (!rbd.passed) anyRoutineFailed = true;
            }
            routineBreakdowns.add(rbd);
        }

        AnnualSubjectData asd = new AnnualSubjectData();
        asd.appeared = appeared;
        asd.totalObtained = totalObtained;
        asd.totalMax = totalMax;
        asd.routineBreakdowns = routineBreakdowns;

        if (appeared && totalMax > 0) {
            asd.scaled = totalObtained.multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalMax), 2, RoundingMode.HALF_UP);
            asd.grade = resolveGrade(asd.scaled.doubleValue(), sortedGrades);
            MarkingStructure refStructure = bundle.sessionStructureMap.get(subjectSessions.get(0).getId());
            boolean aggregatePassed = !anyRoutineFailed && isSubjectPassed(asd.scaled, refStructure.getPassMarks(), asd.grade);
            asd.passed = aggregatePassed;
            if (!asd.passed) asd.grade = getFailGrade(sortedGrades);
        }
        return asd;
    }

    // ─── STATS AGGREGATOR ────────────────────────────────────────────────────────

    private static class StatsAggregator {
        int appeared = 0, passed = 0, failed = 0;
        double sum = 0;
        double highest = Double.MIN_VALUE;
        double lowest = Double.MAX_VALUE;
        Map<String, Integer> gradeDistribution = new LinkedHashMap<>();

        void add(double marks, String gradeName, boolean subjectPassed) {
            appeared++;
            sum += marks;
            if (marks > highest) highest = marks;
            if (marks < lowest) lowest = marks;
            gradeDistribution.merge(gradeName, 1, Integer::sum);
            if (subjectPassed) passed++;
            else failed++;
        }

        Double average() { return appeared > 0 ? Math.round(sum / appeared * 100.0) / 100.0 : null; }
        Double highest() { return appeared > 0 ? Math.round(highest * 100.0) / 100.0 : null; }
        Double lowest()  { return appeared > 0 ? Math.round(lowest  * 100.0) / 100.0 : null; }
    }
}
