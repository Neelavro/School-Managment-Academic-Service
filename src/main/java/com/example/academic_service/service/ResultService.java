package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.Section;
import com.example.academic_service.entity.StudentGroup;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
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

    // ─── SESSION RESULT ──────────────────────────────────────────────────────────

    public SessionResultResponse getSessionResult(Integer examSessionId, Integer genderSectionId, Long sectionId, Integer groupId) {
        ExamSession session = examSessionRepository.findByIdAndIsActiveTrue(examSessionId)
                .orElseThrow(() -> new RuntimeException("Exam session not found: " + examSessionId));

        Class examClass = session.getExamClass();
        Integer classId = examClass.getId();
        Integer examTypeId = session.getExamRoutine().getExamType().getId();
        Integer resolvedGroupId = groupId != null ? groupId : (session.getGroup() != null ? session.getGroup().getId() : null);

        MarkingStructure structure = resolveMarkingStructure(examTypeId, classId, session.getSubject().getId(), resolvedGroupId);
        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, null, genderSectionId, sectionId, resolvedGroupId, null, null);
        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

        List<StudentMark> marks = studentMarkRepository
                .findAllByEnrollmentIdInAndExamSessionIdAndDeletedAtIsNull(enrollmentIds, examSessionId);

        Map<Long, Map<Integer, StudentMark>> markMap = new HashMap<>();
        for (StudentMark m : marks) {
            markMap.computeIfAbsent(m.getEnrollmentId(), k -> new HashMap<>())
                    .put(m.getExamComponent().getId(), m);
        }

        List<Grade> sortedGrades = loadSortedGrades(examClass);

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

                Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
                if (grade != null) {
                    row.setGradeName(grade.getName());
                    row.setGpaValue(grade.getGpaValue());
                }
                row.setPassed(isSubjectPassed(total, structure.getPassMarks(), grade));
            }
            return row;
        }).collect(Collectors.toList());

        studentRows.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));

        SessionResultResponse response = new SessionResultResponse();
        response.setExamSessionId(examSessionId);
        response.setClassName(examClass.getName());
        response.setSubjectName(session.getSubject().getName());
        response.setExamTypeName(session.getExamRoutine().getExamType().getName());
        response.setExamRoutineTitle(session.getExamRoutine().getTitle());
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

        List<ExamSession> sessions = examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this routine and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        // load ALL class enrollments for rank computation
        List<Enrollment> allEnrollments = enrollmentRepository.findAllByClassIdAndFilters(classId, shiftId, null, null, groupId, null, null);
        SessionDataBundle bundle = loadSessionData(sessions, classId, allEnrollments);

        // compute totals for every student in the class
        Map<Long, BigDecimal> totalMarksMap = new HashMap<>();
        Map<Long, Double> gpaMap = new HashMap<>();
        Map<Long, Boolean> passedMap = new HashMap<>();

        for (Enrollment enrollment : allEnrollments) {
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotal = BigDecimal.ZERO;
            boolean overallPassed = true;

            for (ExamSession s : sessions) {
                if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
                MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
                List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
                boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getId(), Collections.emptyMap());
                boolean appeared = !compMarks.isEmpty();
                BigDecimal total = sumComponentMarks(components, compMarks);

                if (appeared) {
                    Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
                    if (!isFourth) {
                        grandTotal = grandTotal.add(total);
                        if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                        if (!isSubjectPassed(total, structure.getPassMarks(), grade)) overallPassed = false;
                    } else if (grade != null) {
                        fourthGpa = grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    overallPassed = false;
                }
            }

            totalMarksMap.put(enrollment.getId(), grandTotal);
            passedMap.put(enrollment.getId(), overallPassed);
            if (!mandatoryGpas.isEmpty())
                gpaMap.put(enrollment.getId(), round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
        }

        // compute rank maps across all scopes
        RankMaps rankMaps = computeRankMaps(allEnrollments, totalMarksMap);

        // build subject infos
        List<RoutineResultResponse.SubjectInfo> subjectInfos = sessions.stream()
                .filter(s -> bundle.sessionStructureMap.containsKey(s.getId()))
                .map(s -> {
                    MarkingStructure ms = bundle.sessionStructureMap.get(s.getId());
                    RoutineResultResponse.SubjectInfo si = new RoutineResultResponse.SubjectInfo();
                    si.setSubjectId(s.getSubject().getId());
                    si.setSubjectName(s.getSubject().getName());
                    si.setFourthSubject(fourthSubjectIds.contains(s.getSubject().getId()));
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

            for (ExamSession s : sessions) {
                if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
                MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
                List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
                boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getId(), Collections.emptyMap());
                boolean appeared = !compMarks.isEmpty();
                BigDecimal total = sumComponentMarks(components, compMarks);

                RoutineResultResponse.SubjectResult sr = new RoutineResultResponse.SubjectResult();
                sr.setSubjectId(s.getSubject().getId());
                sr.setMaxMarks(structure.getTotalMarks());
                sr.setFourthSubject(isFourth);
                sr.setAppeared(appeared);

                if (appeared) {
                    sr.setMarksObtained(total);
                    Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
                    if (grade != null) { sr.setGradeName(grade.getName()); sr.setGpaValue(grade.getGpaValue()); }
                    sr.setPassed(isSubjectPassed(total, structure.getPassMarks(), grade));
                    if (!isFourth) {
                        grandTotal = grandTotal.add(total);
                        if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                        if (!sr.isPassed()) overallPassed = false;
                    } else if (grade != null) {
                        fourthGpa = grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    overallPassed = false;
                }
                subjectResults.add(sr);
            }

            row.setSubjectResults(subjectResults);
            row.setTotalMarks(grandTotal);
            if (!mandatoryGpas.isEmpty()) row.setOverallGpa(round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
            row.setPassed(overallPassed);
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

        List<ExamSession> sessions = examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this academic year and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        // load ALL class enrollments for rank computation
        List<Enrollment> allEnrollments = enrollmentRepository.findAllByClassIdAndFilters(classId, shiftId, null, null, groupId, null, null);
        AnnualDataBundle bundle = loadAnnualData(sessions, classId, allEnrollments);

        Map<Integer, String> subjectNameMap = sessions.stream()
                .collect(Collectors.toMap(s -> s.getSubject().getId(), s -> s.getSubject().getName(), (a, b) -> a));
        List<Integer> orderedSubjectIds = bundle.sessionsBySubject.keySet().stream().sorted().collect(Collectors.toList());

        // compute totals for every student in the class
        Map<Long, BigDecimal> totalScaledMap = new HashMap<>();
        Map<Long, Double> gpaMap = new HashMap<>();
        Map<Long, Boolean> passedMap = new HashMap<>();

        for (Enrollment enrollment : allEnrollments) {
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotalRaw = BigDecimal.ZERO;
            int grandMaxRaw = 0;
            boolean overallPassed = true;

            for (Integer subjectId : orderedSubjectIds) {
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null) continue;
                boolean isFourth = fourthSubjectIds.contains(subjectId);
                if (asd.appeared && asd.totalMax > 0) {
                    if (!isFourth) {
                        grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                        grandMaxRaw += asd.totalMax;
                        if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                        if (!asd.passed) overallPassed = false;
                    } else if (asd.grade != null) {
                        fourthGpa = asd.grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    overallPassed = false;
                }
            }

            BigDecimal scaled = grandMaxRaw > 0
                    ? grandTotalRaw.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(grandMaxRaw), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            totalScaledMap.put(enrollment.getId(), scaled);
            passedMap.put(enrollment.getId(), overallPassed);
            if (!mandatoryGpas.isEmpty())
                gpaMap.put(enrollment.getId(), round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
        }

        // compute rank maps
        RankMaps rankMaps = computeRankMaps(allEnrollments, totalScaledMap);

        List<AnnualResultResponse.SubjectInfo> subjectInfos = orderedSubjectIds.stream()
                .filter(sid -> bundle.sessionsBySubject.get(sid).stream().anyMatch(s -> bundle.sessionStructureMap.containsKey(s.getId())))
                .map(sid -> {
                    AnnualResultResponse.SubjectInfo si = new AnnualResultResponse.SubjectInfo();
                    si.setSubjectId(sid);
                    si.setSubjectName(subjectNameMap.get(sid));
                    si.setFourthSubject(fourthSubjectIds.contains(sid));
                    return si;
                }).collect(Collectors.toList());

        // filter enrollments for display
        List<Enrollment> filteredEnrollments = allEnrollments.stream()
                .filter(e -> matchesFilter(e, genderSectionId, sectionId, groupId, startRoll, endRoll))
                .collect(Collectors.toList());

        List<AnnualResultResponse.StudentResultRow> studentRows = filteredEnrollments.stream().map(enrollment -> {
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

            for (Integer subjectId : orderedSubjectIds) {
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null) continue;
                boolean isFourth = fourthSubjectIds.contains(subjectId);

                AnnualResultResponse.SubjectResult sr = new AnnualResultResponse.SubjectResult();
                sr.setSubjectId(subjectId);
                sr.setMaxMarksRaw(asd.totalMax);
                sr.setFourthSubject(isFourth);
                sr.setAppeared(asd.appeared);

                if (asd.appeared && asd.totalMax > 0) {
                    sr.setMarksRaw(asd.totalObtained);
                    sr.setMarksScaled(asd.scaled);
                    if (asd.grade != null) { sr.setGradeName(asd.grade.getName()); sr.setGpaValue(asd.grade.getGpaValue()); }
                    sr.setPassed(asd.passed);
                    if (!isFourth) {
                        grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                        grandMaxRaw += asd.totalMax;
                        if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                        if (!asd.passed) overallPassed = false;
                    } else if (asd.grade != null) {
                        fourthGpa = asd.grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    overallPassed = false;
                }
                subjectResults.add(sr);
            }

            row.setSubjectResults(subjectResults);
            row.setTotalMarksRaw(grandTotalRaw);
            if (grandMaxRaw > 0) {
                row.setTotalMarksScaled(grandTotalRaw.multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(grandMaxRaw), 2, RoundingMode.HALF_UP));
            }
            if (!mandatoryGpas.isEmpty()) row.setOverallGpa(round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
            row.setPassed(overallPassed);
            return row;
        }).collect(Collectors.toList());

        studentRows.sort(Comparator.comparingInt(r -> r.getClassRoll() != null ? r.getClassRoll() : Integer.MAX_VALUE));

        AnnualResultResponse response = new AnnualResultResponse();
        response.setAcademicYearId(academicYearId);
        response.setAcademicYearName(year.getYearName());
        response.setClassName(examClass.getName());
        response.setUseGpaForResult(Boolean.TRUE.equals(examClass.getUseGpaForResult()));
        response.setSubjects(subjectInfos);
        response.setStudents(studentRows);
        return response;
    }

    // ─── STUDENT ROUTINE RESULT ──────────────────────────────────────────────────

    public StudentRoutineResultResponse getStudentRoutineResult(Long enrollmentId, Integer examRoutineId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));

        Integer classId = enrollment.getStudentClass().getId();
        Integer groupId = enrollment.getStudentGroup() != null ? enrollment.getStudentGroup().getId() : null;

        List<ExamSession> sessions = examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId);

        Class examClass = enrollment.getStudentClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        SessionDataBundle bundle = loadSessionData(sessions, classId, List.of(enrollment));

        List<StudentRoutineResultResponse.SubjectResult> subjectResults = new ArrayList<>();
        List<Double> mandatoryGpas = new ArrayList<>();
        Double fourthGpa = null;
        BigDecimal grandTotal = BigDecimal.ZERO;
        boolean overallPassed = true;

        for (ExamSession s : sessions) {
            if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
            MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> components = bundle.sessionComponentsMap.get(s.getId());
            boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());

            Map<Integer, BigDecimal> compMarks = bundle.markMap
                    .getOrDefault(enrollmentId, Collections.emptyMap())
                    .getOrDefault(s.getId(), Collections.emptyMap());

            boolean appeared = !compMarks.isEmpty();
            BigDecimal total = sumComponentMarks(components, compMarks);

            StudentRoutineResultResponse.SubjectResult sr = new StudentRoutineResultResponse.SubjectResult();
            sr.setSubjectId(s.getSubject().getId());
            sr.setSubjectName(s.getSubject().getName());
            sr.setFourthSubject(isFourth);
            sr.setMaxMarks(structure.getTotalMarks());
            sr.setPassMarks(structure.getPassMarks());
            sr.setAppeared(appeared);

            if (appeared) {
                sr.setMarksObtained(total);
                Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
                if (grade != null) { sr.setGradeName(grade.getName()); sr.setGpaValue(grade.getGpaValue()); }
                sr.setPassed(isSubjectPassed(total, structure.getPassMarks(), grade));

                if (!isFourth) {
                    grandTotal = grandTotal.add(total);
                    if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                    if (!sr.isPassed()) overallPassed = false;
                } else if (grade != null) {
                    fourthGpa = grade.getGpaValue();
                }
            } else if (!isFourth) {
                overallPassed = false;
            }
            subjectResults.add(sr);
        }


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
        if (!mandatoryGpas.isEmpty()) response.setOverallGpa(round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
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

        List<ExamSession> sessions = examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId);

        Class examClass = enrollment.getStudentClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        AnnualDataBundle bundle = loadAnnualData(sessions, classId, List.of(enrollment));
        List<Integer> orderedSubjectIds = bundle.sessionsBySubject.keySet().stream().sorted().collect(Collectors.toList());

        Map<Integer, String> subjectNameMap = sessions.stream()
                .collect(Collectors.toMap(s -> s.getSubject().getId(), s -> s.getSubject().getName(), (a, b) -> a));

        List<StudentAnnualResultResponse.SubjectResult> subjectResults = new ArrayList<>();
        List<Double> mandatoryGpas = new ArrayList<>();
        Double fourthGpa = null;
        BigDecimal grandTotalRaw = BigDecimal.ZERO;
        int grandMaxRaw = 0;
        boolean overallPassed = true;

        for (Integer subjectId : orderedSubjectIds) {
            AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollmentId, sortedGrades);
            if (asd == null) continue;

            boolean isFourth = fourthSubjectIds.contains(subjectId);
            StudentAnnualResultResponse.SubjectResult sr = new StudentAnnualResultResponse.SubjectResult();
            sr.setSubjectId(subjectId);
            sr.setSubjectName(subjectNameMap.getOrDefault(subjectId, ""));
            sr.setFourthSubject(isFourth);
            sr.setMaxMarksRaw(asd.totalMax);
            sr.setAppeared(asd.appeared);

            if (asd.appeared && asd.totalMax > 0) {
                sr.setMarksRaw(asd.totalObtained);
                sr.setMarksScaled(asd.scaled);
                if (asd.grade != null) { sr.setGradeName(asd.grade.getName()); sr.setGpaValue(asd.grade.getGpaValue()); }
                sr.setPassed(asd.passed);

                if (!isFourth) {
                    grandTotalRaw = grandTotalRaw.add(asd.totalObtained);
                    grandMaxRaw += asd.totalMax;
                    if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                    if (!asd.passed) overallPassed = false;
                } else if (asd.grade != null) {
                    fourthGpa = asd.grade.getGpaValue();
                }
            } else if (!isFourth) {
                overallPassed = false;
            }
            subjectResults.add(sr);
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
        response.setSubjectResults(subjectResults);
        response.setTotalMarksRaw(grandTotalRaw);
        if (grandMaxRaw > 0) {
            response.setTotalMarksScaled(grandTotalRaw.multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(grandMaxRaw), 2, RoundingMode.HALF_UP));
        }
        if (!mandatoryGpas.isEmpty()) response.setOverallGpa(round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
        response.setPassed(overallPassed);
        return response;
    }

    // ─── MERIT LIST ──────────────────────────────────────────────────────────────

    public MeritListResponse getMeritList(Integer academicYearId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new RuntimeException("Academic year not found: " + academicYearId));

        List<ExamSession> sessions = examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this academic year and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
        AnnualDataBundle bundle = loadAnnualData(sessions, classId, enrollments);
        List<Integer> orderedSubjectIds = bundle.sessionsBySubject.keySet().stream().sorted().collect(Collectors.toList());

        List<MeritListResponse.MeritEntry> entries = enrollments.stream().map(enrollment -> {
            BigDecimal totalRaw = BigDecimal.ZERO;
            int totalMax = 0;
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            boolean passed = true;

            for (Integer subjectId : orderedSubjectIds) {
                AnnualSubjectData asd = computeAnnualSubjectData(subjectId, bundle, enrollment.getId(), sortedGrades);
                if (asd == null) continue;
                boolean isFourth = fourthSubjectIds.contains(subjectId);

                if (asd.appeared && asd.totalMax > 0) {
                    if (!isFourth) {
                        totalRaw = totalRaw.add(asd.totalObtained);
                        totalMax += asd.totalMax;
                        if (asd.grade != null) mandatoryGpas.add(asd.grade.getGpaValue());
                        if (!asd.passed) passed = false;
                    } else if (asd.grade != null) {
                        fourthGpa = asd.grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    passed = false;
                }
            }

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
            if (!mandatoryGpas.isEmpty()) entry.setOverallGpa(round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
            entry.setPassed(passed);
            return entry;
        }).collect(Collectors.toList());

        entries.sort(Comparator.comparing(
                (MeritListResponse.MeritEntry e) -> e.getTotalMarksRaw() != null ? e.getTotalMarksRaw() : BigDecimal.ZERO,
                Comparator.reverseOrder()));

        int rank = 1;
        for (MeritListResponse.MeritEntry entry : entries) entry.setRank(rank++);

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

        List<ExamSession> sessions = examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this routine and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
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
                        .getOrDefault(s.getId(), Collections.emptyMap());
                if (compMarks.isEmpty()) continue;
                BigDecimal total = sumComponentMarks(components, compMarks);
                Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
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

        List<ExamSession> sessions = examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No sessions found for this academic year");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
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

    public SessionStatsResponse getSessionStats(Integer examSessionId, Integer genderSectionId, Long sectionId, Integer groupId) {
        ExamSession session = examSessionRepository.findByIdAndIsActiveTrue(examSessionId)
                .orElseThrow(() -> new RuntimeException("Exam session not found: " + examSessionId));

        Class examClass = session.getExamClass();
        Integer classId = examClass.getId();
        Integer resolvedGroupId = groupId != null ? groupId : (session.getGroup() != null ? session.getGroup().getId() : null);
        Integer examTypeId = session.getExamRoutine().getExamType().getId();

        MarkingStructure structure = resolveMarkingStructure(examTypeId, classId, session.getSubject().getId(), resolvedGroupId);
        List<MarkingStructureComponent> components =
                markingStructureComponentRepository.findAllByMarkingStructureAndDeletedAtIsNull(structure);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, null, genderSectionId, sectionId, resolvedGroupId, null, null);
        List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

        List<StudentMark> marks = studentMarkRepository
                .findAllByEnrollmentIdInAndExamSessionIdAndDeletedAtIsNull(enrollmentIds, examSessionId);

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
            Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
            boolean subjectPassed = isSubjectPassed(total, structure.getPassMarks(), grade);
            agg.add(total.doubleValue(), grade != null ? grade.getName() : "F", subjectPassed);
        }

        SessionStatsResponse response = new SessionStatsResponse();
        response.setExamSessionId(examSessionId);
        response.setClassName(examClass.getName());
        response.setSubjectName(session.getSubject().getName());
        response.setExamTypeName(session.getExamRoutine().getExamType().getName());
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

        List<ExamSession> sessions = examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No exam sessions found for this routine and class");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> allEnrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, shiftId, null, null, groupId, null, null);
        SessionDataBundle bundle = loadSessionData(sessions, classId, allEnrollments);

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

        // compute total marks for rank computation (mandatory subjects only)
        Map<Long, BigDecimal> totalMarksMap = new HashMap<>();
        for (Enrollment enrollment : allEnrollments) {
            BigDecimal grandTotal = BigDecimal.ZERO;
            for (ExamSession s : sessions) {
                if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
                if (fourthSubjectIds.contains(s.getSubject().getId())) continue;
                List<MarkingStructureComponent> comps = bundle.sessionComponentsMap.get(s.getId());
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getId(), Collections.emptyMap());
                if (!compMarks.isEmpty()) grandTotal = grandTotal.add(sumComponentMarks(comps, compMarks));
            }
            totalMarksMap.put(enrollment.getId(), grandTotal);
        }
        RankMaps rankMaps = computeRankMaps(allEnrollments, totalMarksMap);

        // compute highest marks per session across all students
        Map<Integer, BigDecimal> highestBySession = new HashMap<>();
        for (ExamSession s : sessions) {
            if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
            List<MarkingStructureComponent> comps = bundle.sessionComponentsMap.get(s.getId());
            BigDecimal highest = BigDecimal.ZERO;
            for (Enrollment e : allEnrollments) {
                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(e.getId(), Collections.emptyMap())
                        .getOrDefault(s.getId(), Collections.emptyMap());
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
            MarkingStructure ms = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> comps = bundle.sessionComponentsMap.getOrDefault(s.getId(), Collections.emptyList());

            Map<Integer, Integer> compMaxMarks = new HashMap<>();
            for (MarkingStructureComponent msc : comps) {
                compMaxMarks.put(msc.getExamComponent().getId(), msc.getMaxMarks());
            }

            ProgressReportData.SubjectInfo si = new ProgressReportData.SubjectInfo();
            si.setSubjectId(s.getSubject().getId());
            si.setSubjectName(s.getSubject().getName());
            si.setFourthSubject(fourthSubjectIds.contains(s.getSubject().getId()));
            si.setTotalMarks(ms.getTotalMarks());
            si.setHighestMarks(highestBySession.getOrDefault(s.getId(), BigDecimal.ZERO));
            si.setComponentMaxMarks(compMaxMarks);
            subjectInfos.add(si);
        }

        // filter enrollments for display
        List<Enrollment> filteredEnrollments = allEnrollments.stream()
                .filter(e -> matchesFilter(e, genderSectionId, sectionId, groupId, startRoll, endRoll))
                .collect(Collectors.toList());

        // build student reports
        List<ProgressReportData.StudentReport> studentReports = filteredEnrollments.stream().map(enrollment -> {
            ProgressReportData.StudentReport report = new ProgressReportData.StudentReport();
            report.setEnrollmentId(enrollment.getId());
            report.setStudentSystemId(enrollment.getStudentSystemId());
            report.setClassRoll(enrollment.getClassRoll());

            if (enrollment.getStudent() != null) {
                report.setStudentName(enrollment.getStudent().getNameEnglish());
                report.setFatherName(enrollment.getStudent().getFatherNameEnglish());
                report.setMotherName(enrollment.getStudent().getMotherNameEnglish());
                if (enrollment.getStudent().getImage() != null)
                    report.setImageUrl(enrollment.getStudent().getImage().getImageUrl());
            }
            if (enrollment.getGenderSection() != null) report.setGenderSectionName(enrollment.getGenderSection().getGenderName());
            if (enrollment.getSection() != null) report.setSectionName(enrollment.getSection().getSectionName());
            if (enrollment.getStudentGroup() != null) report.setGroupName(enrollment.getStudentGroup().getGroupName());

            report.setClassRank(rankMaps.classRankMap.get(enrollment.getId()));
            report.setGenderSectionRank(rankMaps.genderSectionRankMap.get(enrollment.getId()));
            report.setSectionRank(rankMaps.sectionRankMap.get(enrollment.getId()));
            report.setGroupRank(rankMaps.groupRankMap.get(enrollment.getId()));

            List<ProgressReportData.SubjectResult> subjectResults = new ArrayList<>();
            List<Double> mandatoryGpas = new ArrayList<>();
            Double fourthGpa = null;
            BigDecimal grandTotal = BigDecimal.ZERO;
            boolean overallPassed = true;
            int failedCount = 0;

            for (ExamSession s : sessions) {
                if (!bundle.sessionStructureMap.containsKey(s.getId())) continue;
                MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
                List<MarkingStructureComponent> comps = bundle.sessionComponentsMap.get(s.getId());
                boolean isFourth = fourthSubjectIds.contains(s.getSubject().getId());

                Map<Integer, BigDecimal> compMarks = bundle.markMap
                        .getOrDefault(enrollment.getId(), Collections.emptyMap())
                        .getOrDefault(s.getId(), Collections.emptyMap());
                boolean appeared = !compMarks.isEmpty();
                BigDecimal total = sumComponentMarks(comps, compMarks);

                ProgressReportData.SubjectResult sr = new ProgressReportData.SubjectResult();
                sr.setSubjectId(s.getSubject().getId());
                sr.setFourthSubject(isFourth);
                sr.setAppeared(appeared);
                sr.setComponentMarks(new HashMap<>(compMarks));

                if (appeared) {
                    sr.setTotalMarks(total);
                    Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
                    if (grade != null) { sr.setGradeName(grade.getName()); sr.setGpaValue(grade.getGpaValue()); }
                    sr.setPassed(isSubjectPassed(total, structure.getPassMarks(), grade));
                    if (!isFourth) {
                        grandTotal = grandTotal.add(total);
                        if (grade != null) mandatoryGpas.add(grade.getGpaValue());
                        if (!sr.isPassed()) { overallPassed = false; failedCount++; }
                    } else if (grade != null) {
                        fourthGpa = grade.getGpaValue();
                    }
                } else if (!isFourth) {
                    overallPassed = false;
                    failedCount++;
                }
                subjectResults.add(sr);
            }

            report.setSubjectResults(subjectResults);
            report.setTotalMarks(grandTotal);
            report.setPassed(overallPassed);
            report.setFailedSubjectCount(failedCount);

            if (!mandatoryGpas.isEmpty()) {
                double sum = mandatoryGpas.stream().mapToDouble(Double::doubleValue).sum();
                report.setGpaWithout4th(round2(Math.min(5.0, sum / mandatoryGpas.size())));
                report.setOverallGpa(round2(computeOverallGpa(mandatoryGpas, fourthGpa)));
            }
            return report;
        }).collect(Collectors.toList());

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

    // ─── STATS – ROUTINE ─────────────────────────────────────────────────────────

    public RoutineStatsResponse getRoutineStats(Integer examRoutineId, Integer classId, Integer shiftId, Integer genderSectionId, Long sectionId, Integer groupId, Integer startRoll, Integer endRoll) {
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));

        List<ExamSession> sessions = examSessionRepository
                .findForRoutineAndClassWithGroupFilter(examRoutineId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No sessions found");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
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
                        .getOrDefault(s.getId(), Collections.emptyMap());
                if (compMarks.isEmpty()) continue;
                BigDecimal total = sumComponentMarks(components, compMarks);
                Grade grade = resolveGrade(total.doubleValue(), sortedGrades);
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

        List<ExamSession> sessions = examSessionRepository
                .findForAnnualByClassWithGroupFilter(academicYearId, classId, groupId);
        if (sessions.isEmpty()) throw new RuntimeException("No sessions found for this academic year");

        Class examClass = sessions.get(0).getExamClass();
        List<Grade> sortedGrades = loadSortedGrades(examClass);
        Set<Integer> fourthSubjectIds = loadFourthSubjectIds(classId, groupId);

        List<Enrollment> enrollments = enrollmentRepository
                .findAllByClassIdAndFilters(classId, shiftId, genderSectionId, sectionId, groupId, startRoll, endRoll);
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

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

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

    private List<Grade> loadSortedGrades(Class examClass) {
        if (examClass.getGradingPolicy() == null) return Collections.emptyList();
        List<Grade> grades = gradeRepository.findByGradingPolicyId(examClass.getGradingPolicy().getId());
        grades.sort(Comparator.comparingDouble(Grade::getMinMark).reversed());
        return grades;
    }

    private Set<Integer> loadFourthSubjectIds(Integer classId, Integer groupId) {
        List<ClassSubjectGroup> groups = groupId != null
                ? classSubjectGroupRepository.findSubjectsForStudent(classId, groupId)
                : classSubjectGroupRepository.findByStudentClassIdAndIsActiveTrue(classId);
        return groups.stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsFourthSubject()))
                .map(g -> g.getSubject().getId())
                .collect(Collectors.toSet());
    }

    private MarkingStructure resolveMarkingStructure(Integer examTypeId, Integer classId, Integer subjectId, Integer groupId) {
        List<MarkingStructure> structures = markingStructureRepository
                .findAllByFiltersAndDeletedAtIsNull(examTypeId, classId, subjectId, groupId);
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
                .map(c -> compMarks.getOrDefault(c.getExamComponent().getId(), BigDecimal.ZERO))
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

    private RankMaps computeRankMaps(List<Enrollment> allEnrollments, Map<Long, BigDecimal> totalMarksMap) {
        RankMaps rm = new RankMaps();
        Comparator<Enrollment> byMarksDesc = Comparator.comparing(
                e -> totalMarksMap.getOrDefault(e.getId(), BigDecimal.ZERO), Comparator.reverseOrder());

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

    // ─── DATA BUNDLE HELPERS ─────────────────────────────────────────────────────

    private static class SessionDataBundle {
        List<Enrollment> enrollments;
        Map<Integer, MarkingStructure> sessionStructureMap = new HashMap<>();
        Map<Integer, List<MarkingStructureComponent>> sessionComponentsMap = new HashMap<>();
        // enrollmentId -> sessionId -> componentId -> marks
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

        if (!enrollments.isEmpty()) {
            List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
            List<Integer> sessionIds = sessions.stream().map(ExamSession::getId).collect(Collectors.toList());
            List<StudentMark> allMarks = studentMarkRepository
                    .findAllByEnrollmentIdsAndSessionIds(enrollmentIds, sessionIds);
            for (StudentMark m : allMarks) {
                bundle.markMap.computeIfAbsent(m.getEnrollmentId(), k -> new HashMap<>())
                        .computeIfAbsent(m.getExamSession().getId(), k -> new HashMap<>())
                        .put(m.getExamComponent().getId(), m.getMarksObtained());
            }
        }
        return bundle;
    }

    private static class AnnualDataBundle {
        Map<Integer, List<ExamSession>> sessionsBySubject;
        Map<Integer, MarkingStructure> sessionStructureMap = new HashMap<>();
        Map<Integer, List<MarkingStructureComponent>> sessionComponentsMap = new HashMap<>();
        // enrollmentId -> sessionId -> componentId -> marks
        Map<Long, Map<Integer, Map<Integer, BigDecimal>>> markMap = new HashMap<>();
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

        if (!enrollments.isEmpty()) {
            List<Long> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
            List<Integer> sessionIds = sessions.stream().map(ExamSession::getId).collect(Collectors.toList());
            List<StudentMark> allMarks = studentMarkRepository
                    .findAllByEnrollmentIdsAndSessionIds(enrollmentIds, sessionIds);
            for (StudentMark m : allMarks) {
                bundle.markMap.computeIfAbsent(m.getEnrollmentId(), k -> new HashMap<>())
                        .computeIfAbsent(m.getExamSession().getId(), k -> new HashMap<>())
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
    }

    private AnnualSubjectData computeAnnualSubjectData(Integer subjectId, AnnualDataBundle bundle, Long enrollmentId, List<Grade> sortedGrades) {
        List<ExamSession> subjectSessions = bundle.sessionsBySubject.getOrDefault(subjectId, Collections.emptyList())
                .stream().filter(s -> bundle.sessionStructureMap.containsKey(s.getId())).collect(Collectors.toList());
        if (subjectSessions.isEmpty()) return null;

        boolean appeared = false;
        BigDecimal totalObtained = BigDecimal.ZERO;
        int totalMax = 0;

        for (ExamSession s : subjectSessions) {
            MarkingStructure structure = bundle.sessionStructureMap.get(s.getId());
            List<MarkingStructureComponent> components = bundle.sessionComponentsMap.getOrDefault(s.getId(), Collections.emptyList());
            Map<Integer, BigDecimal> compMarks = bundle.markMap
                    .getOrDefault(enrollmentId, Collections.emptyMap())
                    .getOrDefault(s.getId(), Collections.emptyMap());

            if (!compMarks.isEmpty()) appeared = true;
            totalObtained = totalObtained.add(sumComponentMarks(components, compMarks));
            totalMax += structure.getTotalMarks();
        }

        AnnualSubjectData asd = new AnnualSubjectData();
        asd.appeared = appeared;
        asd.totalObtained = totalObtained;
        asd.totalMax = totalMax;

        if (appeared && totalMax > 0) {
            asd.scaled = totalObtained.multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalMax), 2, RoundingMode.HALF_UP);
            asd.grade = resolveGrade(asd.scaled.doubleValue(), sortedGrades);
            MarkingStructure refStructure = bundle.sessionStructureMap.get(subjectSessions.get(0).getId());
            asd.passed = isSubjectPassed(asd.scaled, refStructure.getPassMarks(), asd.grade);
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
