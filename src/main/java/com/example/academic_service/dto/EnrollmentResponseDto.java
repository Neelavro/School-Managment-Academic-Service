package com.example.academic_service.dto;

import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.entity.Student;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentResponseDto {

    // ── From student-service ──────────────────────────────────────────────────
    private Long id;
    private Long enrollmentId;
    private String studentSystemId;
    private String nameEnglish;
    private String nameBangla;
    private String fatherNameEnglish;
    private String fatherNameBangla;
    private String fatherOccupation;
    private String fatherPhone;
    private String fatherMonthlySalary;
    private String motherNameEnglish;
    private String motherNameBangla;
    private String motherOccupation;
    private String motherPhone;
    private String motherMonthlySalary;
    private String guardianNameEnglish;
    private String guardianNameBangla;
    private String guardianOccupation;
    private String guardianPhone;
    private String guardianRelation;
    private String currentHoldingNo;
    private String currentRoadOrVillage;
    private String currentDistrict;
    private String currentThana;
    private String permanentHoldingNo;
    private String permanentRoadOrVillage;
    private String permanentDistrict;
    private String permanentThana;
    private String dob;
    private String nationality;
    private Boolean isActive;
    private StudentDto.GenderDto gender;
    private StudentDto.StudentStatusDto studentStatus;
    private StudentDto.StudentImageDto image;

    // ── From academic-service (enrollment) ───────────────────────────────────
    private Integer classRoll;
    private AcademicYearDto academicYear;
    private StudentClassDto studentClass;
    private SectionDto section;
    private ShiftDto shift;
    private GenderSectionDto genderSection;
    private StudentGroupDto studentGroup;

    // ── Nested DTOs ───────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class AcademicYearDto {
        private Integer id;
        private String yearName;
        private Boolean isActive;

        public static AcademicYearDto from(AcademicYear e) {
            if (e == null) return null;
            AcademicYearDto dto = new AcademicYearDto();
            dto.setId(e.getId());
            dto.setYearName(e.getYearName());
            dto.setIsActive(e.getIsActive());
            return dto;
        }
    }

    @Getter
    @Setter
    public static class ShiftDto {
        private Integer id;
        private String name;
        private Boolean isActive;

        public static ShiftDto from(Shift e) {
            if (e == null) return null;
            ShiftDto dto = new ShiftDto();
            dto.setId(e.getId());
            dto.setName(e.getName());
            dto.setIsActive(e.getIsActive());
            return dto;
        }
    }

    @Getter
    @Setter
    public static class GenderSectionDto {
        private Integer id;
        private String genderName;

        public static GenderSectionDto from(GenderSection e) {
            if (e == null) return null;
            GenderSectionDto dto = new GenderSectionDto();
            dto.setId(e.getId());
            dto.setGenderName(e.getGenderName());
            return dto;
        }
    }

    @Getter
    @Setter
    public static class StudentGroupDto {
        private Integer id;
        private String name;

        public static StudentGroupDto from(StudentGroup e) {
            if (e == null) return null;
            StudentGroupDto dto = new StudentGroupDto();
            dto.setId(e.getId());
            dto.setName(e.getGroupName());
            return dto;
        }
    }

    @Getter
    @Setter
    public static class StudentClassDto {
        private Integer id;
        private String name;
        private Integer orderIndex;
        private Boolean isActive;
        private ShiftDto shift;

        public static StudentClassDto from(Class e) {
            if (e == null) return null;
            StudentClassDto dto = new StudentClassDto();
            dto.setId(e.getId());
            dto.setName(e.getName());
            dto.setOrderIndex(e.getOrderIndex());
            dto.setIsActive(e.getIsActive());
            dto.setShift(ShiftDto.from(e.getShift()));
            return dto;
        }
    }

    @Getter
    @Setter
    public static class SectionDto {
        private Long id;
        private String sectionName;
        private Boolean isActive;
        private StudentClassDto classEntity;
        private GenderSectionDto genderSection;

        public static SectionDto from(Section e) {
            if (e == null) return null;
            SectionDto dto = new SectionDto();
            dto.setId(e.getId());
            dto.setSectionName(e.getSectionName());
            dto.setIsActive(e.getIsActive());
            dto.setClassEntity(StudentClassDto.from(e.getClassEntity()));
            dto.setGenderSection(GenderSectionDto.from(e.getGenderSection()));
            return dto;
        }
    }

    // ── Static factory ────────────────────────────────────────────────────────

    public static EnrollmentResponseDto from(Enrollment enrollment, Student student) {
        EnrollmentResponseDto dto = new EnrollmentResponseDto();

        // Academic fields from enrollment
        dto.setEnrollmentId(enrollment.getId());
        dto.setClassRoll(enrollment.getClassRoll());
        dto.setAcademicYear(AcademicYearDto.from(enrollment.getAcademicYear()));
        dto.setStudentClass(StudentClassDto.from(enrollment.getStudentClass()));
        dto.setSection(SectionDto.from(enrollment.getSection()));
        dto.setShift(ShiftDto.from(enrollment.getShift()));
        dto.setGenderSection(GenderSectionDto.from(enrollment.getGenderSection()));
        dto.setStudentGroup(StudentGroupDto.from(enrollment.getStudentGroup()));

        // Student personal fields
        dto.setId(student.getId());
        dto.setStudentSystemId(student.getStudentSystemId());
        dto.setNameEnglish(student.getNameEnglish());
        dto.setNameBangla(student.getNameBangla());
        dto.setFatherNameEnglish(student.getFatherNameEnglish());
        dto.setFatherNameBangla(student.getFatherNameBangla());
        dto.setFatherOccupation(student.getFatherOccupation());
        dto.setFatherPhone(student.getFatherPhone());
        dto.setFatherMonthlySalary(student.getFatherMonthlySalary());
        dto.setMotherNameEnglish(student.getMotherNameEnglish());
        dto.setMotherNameBangla(student.getMotherNameBangla());
        dto.setMotherOccupation(student.getMotherOccupation());
        dto.setMotherPhone(student.getMotherPhone());
        dto.setMotherMonthlySalary(student.getMotherMonthlySalary());
        dto.setGuardianNameEnglish(student.getGuardianNameEnglish());
        dto.setGuardianNameBangla(student.getGuardianNameBangla());
        dto.setGuardianOccupation(student.getGuardianOccupation());
        dto.setGuardianPhone(student.getGuardianPhone());
        dto.setGuardianRelation(student.getGuardianRelation());
        dto.setCurrentHoldingNo(student.getCurrentHoldingNo());
        dto.setCurrentRoadOrVillage(student.getCurrentRoadOrVillage());
        dto.setCurrentDistrict(student.getCurrentDistrict());
        dto.setCurrentThana(student.getCurrentThana());
        dto.setPermanentHoldingNo(student.getPermanentHoldingNo());
        dto.setPermanentRoadOrVillage(student.getPermanentRoadOrVillage());
        dto.setPermanentDistrict(student.getPermanentDistrict());
        dto.setPermanentThana(student.getPermanentThana());
        dto.setDob(student.getDob() != null ? student.getDob().toString() : null);
        dto.setNationality(student.getNationality());
        dto.setIsActive(student.getIsActive());

        // Nested DTOs mapped from entity relations
        if (student.getGender() != null) {
            StudentDto.GenderDto g = new StudentDto.GenderDto();
            g.setId(student.getGender().getId());
            g.setGender(student.getGender().getGender());
            g.setIsActive(student.getGender().getIsActive());
            dto.setGender(g);
        }
        if (student.getStudentStatus() != null) {
            StudentDto.StudentStatusDto s = new StudentDto.StudentStatusDto();
            s.setId(student.getStudentStatus().getId());
            s.setStatusName(student.getStudentStatus().getStatusName());
            dto.setStudentStatus(s);
        }
        if (student.getImage() != null) {
            StudentDto.StudentImageDto img = new StudentDto.StudentImageDto();
            img.setId((long) student.getImage().getId());
            img.setImageUrl(student.getImage().getImageUrl());
            img.setIsActive(student.getImage().getIsActive());
            dto.setImage(img);
        }

        return dto;
    }
}