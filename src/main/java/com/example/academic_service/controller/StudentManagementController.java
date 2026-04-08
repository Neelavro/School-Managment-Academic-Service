package com.example.academic_service.controller;

import com.example.academic_service.entity.*;
import com.example.academic_service.service.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student-management")
public class StudentManagementController {

    private final GenderSectionService genderSectionService;
    private final AcademicYearService academicYearService;
    private final ShiftService shiftService;
    private final StudentGroupService studentGroupService;
    private final GenderService genderService;
    private final EnrollmentService enrollmentService;

    public StudentManagementController(
            GenderSectionService genderSectionService,
            AcademicYearService academicYearService,
            ShiftService shiftService,
            StudentGroupService studentGroupService,
            GenderService genderService,
            EnrollmentService enrollmentService
    ) {
        this.genderSectionService = genderSectionService;
        this.academicYearService = academicYearService;
        this.shiftService = shiftService;
        this.studentGroupService = studentGroupService;
        this.genderService = genderService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/init")
    public ResponseEntity<StudentManagementInitResponse> getInitialData() {
        List<GenderSection> sections = genderSectionService.getAllGenderSections();
        List<AcademicYear> academicYears = academicYearService.getAllAcademicYears();
        List<Shift> shifts = shiftService.getAllActiveShifts();
        List<StudentGroup> studentGroups = studentGroupService.getAllActiveGroups();
        List<Gender> genders = genderService.getAllGenders();
        List<StudentStatus> statuses = enrollmentService.getStudentStatus();

        StudentManagementInitResponse response = new StudentManagementInitResponse(
                sections,
                academicYears,
                shifts,
                studentGroups,
                genders,
                statuses
        );

        return ResponseEntity.ok(response);
    }

    // ---------------- DTOs ----------------

    @Getter
    @Setter
    @AllArgsConstructor
    public static class StudentManagementInitResponse {
        private List<GenderSection> sections;
        private List<AcademicYear> academicYears;
        private List<Shift> shifts;
        private List<StudentGroup> studentGroups;
        private List<Gender> genders;
        private List<StudentStatus> statuses;
    }
}
