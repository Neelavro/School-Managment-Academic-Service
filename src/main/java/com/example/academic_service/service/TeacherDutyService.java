package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.duty_dtos.*;
import com.example.academic_service.dto.schedule_dtos.ClassRoutineResponseDto;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDutyService {

    private final StaffRepository staffRepository;
    private final ClassRoutineRepository classRoutineRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamClassRoomAssignmentRepository examClassRoomAssignmentRepository;
    private final RoomRepository roomRepository;
    private final TeacherPeriodDutyRepository periodDutyRepository;
    private final TeacherExamDutyRepository examDutyRepository;

    // ─── Teaching staff ────────────────────────────────────────────────────────

    public ApiResponse<List<?>> getTeachingStaff() {
        List<Staff> staff = staffRepository.findByIsActiveAndEmployeeType(true, EmployeeType.TEACHING);
        List<Object> result = staff.stream()
                .map(s -> new java.util.LinkedHashMap<String, Object>() {{
                    put("id", s.getId());
                    put("staffSystemId", s.getStaffSystemId());
                    put("nameEnglish", s.getNameEnglish());
                    put("nameBangla", s.getNameBangla());
                    put("currentDesignationName", s.getCurrentDesignation() != null ? s.getCurrentDesignation().getName() : null);
                }})
                .collect(Collectors.toList());
        return new ApiResponse<>("OK", result);
    }

    // ─── Class routines (for duty assignment picker) ───────────────────────────

    public ApiResponse<List<ClassRoutineResponseDto>> getActiveClassRoutines() {
        List<ClassRoutineResponseDto> routines = classRoutineRepository
                .findByRoutineTypeAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(RoutineType.DEFAULT)
                .stream().map(ClassRoutineResponseDto::from).collect(Collectors.toList());
        return new ApiResponse<>("OK", routines);
    }

    // ─── Period duties ─────────────────────────────────────────────────────────

    public ApiResponse<List<TeacherPeriodDutyResponseDto>> getPeriodDuties(Long staffId) {
        List<TeacherPeriodDutyResponseDto> result = periodDutyRepository.findByStaff_Id(staffId)
                .stream().map(TeacherPeriodDutyResponseDto::from).collect(Collectors.toList());
        return new ApiResponse<>("OK", result);
    }

    @Transactional
    public ApiResponse<List<TeacherPeriodDutyResponseDto>> assignPeriodDuties(Long staffId, TeacherPeriodDutyBulkRequestDto dto) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        if (staff.getEmployeeType() != EmployeeType.TEACHING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only teaching staff can be assigned class period duties");
        }

        for (Integer routineId : dto.getClassRoutineIds()) {
            ClassRoutine routine = classRoutineRepository.findById(routineId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class routine " + routineId + " not found"));

            if (!Boolean.TRUE.equals(routine.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class routine " + routineId + " is not active");
            }

            if (periodDutyRepository.existsByClassRoutine_Id(routineId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This period already has a teacher assigned: " + routine.getClassEntity().getName()
                                + " " + routine.getDayOfWeek() + " " + routine.getStartTime());
            }

            List<TeacherPeriodDuty> conflicts = periodDutyRepository.findConflicts(
                    staffId, routine.getDayOfWeek(), routine.getStartTime(), routine.getEndTime());
            if (!conflicts.isEmpty()) {
                ClassRoutine conflicting = conflicts.get(0).getClassRoutine();
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Time conflict on " + routine.getDayOfWeek() + " " + routine.getStartTime() + "–" + routine.getEndTime()
                                + " with existing duty: " + conflicting.getClassEntity().getName()
                                + " (" + conflicting.getStartTime() + "–" + conflicting.getEndTime() + ")");
            }

            TeacherPeriodDuty duty = new TeacherPeriodDuty();
            duty.setStaff(staff);
            duty.setClassRoutine(routine);
            periodDutyRepository.save(duty);
        }

        List<TeacherPeriodDutyResponseDto> result = periodDutyRepository.findByStaff_Id(staffId)
                .stream().map(TeacherPeriodDutyResponseDto::from).collect(Collectors.toList());
        return new ApiResponse<>("Period duties assigned", result);
    }

    public ApiResponse<Void> removePeriodDuty(Long id) {
        if (!periodDutyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Period duty not found");
        }
        periodDutyRepository.deleteById(id);
        return new ApiResponse<>("Removed", null);
    }

    // ─── Exam duties ───────────────────────────────────────────────────────────

    public ApiResponse<List<TeacherExamDutyResponseDto>> getExamDuties(Long staffId) {
        List<TeacherExamDutyResponseDto> result = examDutyRepository.findByStaff_Id(staffId)
                .stream().map(TeacherExamDutyResponseDto::from).collect(Collectors.toList());
        return new ApiResponse<>("OK", result);
    }

    @Transactional
    public ApiResponse<List<TeacherExamDutyResponseDto>> assignExamDuties(Long staffId, TeacherExamDutyBulkRequestDto dto) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        if (staff.getEmployeeType() != EmployeeType.TEACHING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only teaching staff can be assigned exam duties");
        }

        for (ExamDutyItemDto item : dto.getAssignments()) {
            ExamSession session = examSessionRepository.findByIdAndIsActiveTrue(item.getExamSessionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Exam session " + item.getExamSessionId() + " not found"));

            if (session.getDate() == null || session.getStartTime() == null || session.getEndTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Exam session " + item.getExamSessionId() + " does not have a defined date/time");
            }

            List<ExamClassRoomAssignment> validAssignments = examClassRoomAssignmentRepository
                    .findByExamRoutineIdAndExamClassId(session.getExamRoutine().getId(), session.getExamClass().getId());
            boolean roomValid = validAssignments.stream()
                    .anyMatch(a -> a.getRoom().getId().equals(item.getRoomId()));
            if (!roomValid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Room " + item.getRoomId() + " is not assigned to "
                                + session.getExamClass().getName() + " for this exam routine");
            }

            if (examDutyRepository.existsByStaff_IdAndExamSession_Id(staffId, item.getExamSessionId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Teacher is already assigned to exam session: "
                                + session.getExamClass().getName() + " " + session.getSubject().getName()
                                + " on " + session.getDate());
            }

            List<TeacherExamDuty> conflicts = examDutyRepository.findConflicts(
                    staffId, session.getDate(), session.getStartTime(), session.getEndTime());
            if (!conflicts.isEmpty()) {
                ExamSession conflicting = conflicts.get(0).getExamSession();
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Exam time conflict on " + session.getDate() + " " + session.getStartTime() + "–" + session.getEndTime()
                                + " with existing duty: " + conflicting.getExamClass().getName()
                                + " " + conflicting.getSubject().getName());
            }

            Room room = roomRepository.getReferenceById(item.getRoomId());
            TeacherExamDuty duty = new TeacherExamDuty();
            duty.setStaff(staff);
            duty.setExamSession(session);
            duty.setRoom(room);
            examDutyRepository.save(duty);
        }

        List<TeacherExamDutyResponseDto> result = examDutyRepository.findByStaff_Id(staffId)
                .stream().map(TeacherExamDutyResponseDto::from).collect(Collectors.toList());
        return new ApiResponse<>("Exam duties assigned", result);
    }

    public ApiResponse<Void> removeExamDuty(Long id) {
        if (!examDutyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam duty not found");
        }
        examDutyRepository.deleteById(id);
        return new ApiResponse<>("Removed", null);
    }

    // ─── Available rooms for exam session ──────────────────────────────────────

    public ApiResponse<List<AvailableRoomDto>> getAvailableRoomsForExamSession(Integer examSessionId) {
        ExamSession session = examSessionRepository.findByIdAndIsActiveTrue(examSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam session not found"));
        List<AvailableRoomDto> rooms = examClassRoomAssignmentRepository
                .findByExamRoutineIdAndExamClassId(session.getExamRoutine().getId(), session.getExamClass().getId())
                .stream().map(a -> AvailableRoomDto.from(a.getRoom())).collect(Collectors.toList());
        return new ApiResponse<>("OK", rooms);
    }

    // ─── Available exam sessions (with defined time slots) ─────────────────────

    public ApiResponse<List<ExamSessionForDutyDto>> getAvailableExamSessions() {
        List<ExamSessionForDutyDto> sessions = examSessionRepository.findAllWithDefinedTimeSlots()
                .stream().map(ExamSessionForDutyDto::from).collect(Collectors.toList());
        return new ApiResponse<>("OK", sessions);
    }
}
