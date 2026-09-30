package com.example.academic_service.service;

import com.example.academic_service.dto.result_dtos.AnnualResultResponse;
import com.example.academic_service.dto.result_dtos.MeritBoardResponse;
import com.example.academic_service.dto.result_dtos.RoutineResultResponse;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.repository.ExamRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Merit lists for the admin panel, for one exam or for the year.
 *
 * Nothing is ranked here: totals, GPA, pass/fail and the class, shift and section positions are the routine and
 * annual results' own (ResultService, ranked by MeritRanking), so the merit list always agrees with those pages
 * and their PDFs. This only reshapes them into the list the page and its PDF draw.
 */
@Service
@RequiredArgsConstructor
public class MeritBoardService {

    private final ResultService resultService;
    private final ExamRoutineRepository examRoutineRepository;

    @Transactional(readOnly = true)
    public MeritBoardResponse forRoutine(Integer examRoutineId, Integer classId) {
        ExamRoutine routine = examRoutineRepository.findById(examRoutineId)
                .orElseThrow(() -> new RuntimeException("Exam routine not found: " + examRoutineId));
        RoutineResultResponse result = resultService.getRoutineResult(examRoutineId, classId, null, null, null, null, null, null);

        List<MeritBoardResponse.Entry> entries = new ArrayList<>();
        for (RoutineResultResponse.StudentResultRow r : result.getStudents()) {
            RoutineResultResponse.Merit m = r.getMerit();
            MeritBoardResponse.Entry e = entry(r.getEnrollmentId(), r.getStudentSystemId(), r.getStudentName(), r.getClassRoll(),
                    r.getGroupId(), r.getGroupName(), r.getGenderSectionId(), r.getGenderSectionName(),
                    r.getSectionId(), r.getSectionName());
            e.setTotalMarks(r.getTotalMarks());
            e.setOverallGpa(r.getOverallGpa());
            e.setPassed(r.isPassed());
            if (m != null) {
                e.setClassPosition(m.getClassRank());
                e.setShiftPosition(m.getGenderSectionRank());
                e.setSectionPosition(m.getSectionRank());
            }
            entries.add(e);
        }

        MeritBoardResponse board = new MeritBoardResponse();
        board.setClassName(result.getClassName());
        board.setAcademicYearName(routine.getAcademicYear() != null ? routine.getAcademicYear().getYearName() : null);
        board.setTitle(result.getRoutineTitle());
        board.setExamTypeName(result.getExamTypeName());
        board.setUseGpaForResult(result.isUseGpaForResult());
        return finish(board, entries);
    }

    @Transactional(readOnly = true)
    public MeritBoardResponse forYear(Integer academicYearId, Integer classId) {
        AnnualResultResponse result = resultService.getAnnualResult(academicYearId, classId, null, null, null, null, null, null);

        List<MeritBoardResponse.Entry> entries = new ArrayList<>();
        for (AnnualResultResponse.StudentResultRow r : result.getStudents()) {
            AnnualResultResponse.Merit m = r.getMerit();
            MeritBoardResponse.Entry e = entry(r.getEnrollmentId(), r.getStudentSystemId(), r.getStudentName(), r.getClassRoll(),
                    r.getGroupId(), r.getGroupName(), r.getGenderSectionId(), r.getGenderSectionName(),
                    r.getSectionId(), r.getSectionName());
            // The annual results rank by the scaled total, so that is the total shown.
            e.setTotalMarks(r.getTotalMarksScaled() != null ? r.getTotalMarksScaled() : r.getTotalMarksRaw());
            e.setOverallGpa(r.getOverallGpa());
            e.setPassed(r.isPassed());
            if (m != null) {
                e.setClassPosition(m.getClassRank());
                e.setShiftPosition(m.getGenderSectionRank());
                e.setSectionPosition(m.getSectionRank());
            }
            entries.add(e);
        }

        MeritBoardResponse board = new MeritBoardResponse();
        board.setClassName(result.getClassName());
        board.setAcademicYearName(result.getAcademicYearName());
        board.setTitle("Annual");
        board.setUseGpaForResult(result.isUseGpaForResult());
        return finish(board, entries);
    }

    private static MeritBoardResponse.Entry entry(Long enrollmentId, String sid, String name, Integer roll,
                                                  Integer groupId, String groupName, Integer genderSectionId,
                                                  String genderSectionName, Long sectionId, String sectionName) {
        MeritBoardResponse.Entry e = new MeritBoardResponse.Entry();
        e.setEnrollmentId(enrollmentId);
        e.setStudentSystemId(sid);
        e.setStudentName(name);
        e.setClassRoll(roll);
        e.setGroupId(groupId);
        e.setGroupName(groupName);
        e.setGenderSectionId(genderSectionId);
        e.setGenderSectionName(genderSectionName);
        e.setSectionId(sectionId);
        e.setSectionName(sectionName);
        return e;
    }

    /** Flags for the page's tabs, and the order: group by group, passed by class position, then failed by roll. */
    private static MeritBoardResponse finish(MeritBoardResponse board, List<MeritBoardResponse.Entry> entries) {
        List<MeritBoardResponse.Entry> byRoll = new ArrayList<>(entries);
        byRoll.sort(Comparator.comparing((MeritBoardResponse.Entry e) -> e.getClassRoll() != null ? e.getClassRoll() : Integer.MAX_VALUE)
                .thenComparing(MeritBoardResponse.Entry::getEnrollmentId));
        Map<String, Integer> groupOrder = new HashMap<>();
        for (MeritBoardResponse.Entry e : byRoll) groupOrder.putIfAbsent(Objects.toString(e.getGroupId()), groupOrder.size());
        List<MeritBoardResponse.Entry> ordered = new ArrayList<>(byRoll);
        ordered.sort(Comparator.comparing((MeritBoardResponse.Entry e) -> groupOrder.get(Objects.toString(e.getGroupId())))
                .thenComparing(e -> e.getClassPosition() != null ? 0 : 1)
                .thenComparing(e -> e.getClassPosition() != null ? e.getClassPosition() : Integer.MAX_VALUE));

        board.setHasGroups(entries.stream().anyMatch(e -> e.getGroupId() != null));
        // The ranking gives shift positions only when the class has more than one gender section.
        board.setHasShifts(entries.stream().anyMatch(e -> e.getShiftPosition() != null));
        board.setHasSections(entries.stream().anyMatch(e -> e.getSectionId() != null));
        board.setEntries(ordered);
        return board;
    }
}
