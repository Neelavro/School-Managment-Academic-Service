package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassTeacherService {

    private final ClassTeacherRepository classTeacherRepository;
    private final StaffRepository staffRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearRepository academicYearRepository;

    public List<Map<String, Object>> getByYear(Integer academicYearId) {
        return classTeacherRepository.findAllByAcademicYearId(academicYearId)
                .stream().map(this::toMap).toList();
    }

    public Map<String, Object> assign(Long staffId, Long sectionId, Integer academicYearId) {
        if (classTeacherRepository.existsBySectionIdAndAcademicYearId(sectionId, academicYearId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This section already has a class teacher for this academic year");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        Section section = sectionRepository.findById(Math.toIntExact(sectionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));

        ClassTeacher ct = new ClassTeacher();
        ct.setStaff(staff);
        ct.setSection(section);
        ct.setAcademicYear(year);
        return toMap(classTeacherRepository.save(ct));
    }

    public void remove(Long id) {
        if (!classTeacherRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        classTeacherRepository.deleteById(id);
    }

    // Used by teacher portal & attendance controller
    public ClassTeacher getByStaffAndYear(Long staffId, Integer academicYearId) {
        return classTeacherRepository.findByStaffIdAndAcademicYearId(staffId, academicYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "You are not assigned as a class teacher for this academic year"));
    }

    public Map<String, Object> getMyAssignment(Long staffId, Integer academicYearId) {
        return toMap(getByStaffAndYear(staffId, academicYearId));
    }

    private Map<String, Object> toMap(ClassTeacher ct) {
        Class cls = ct.getSection().getClassEntity();
        return Map.of(
                "id", ct.getId(),
                "staffId", ct.getStaff().getId(),
                "staffName", ct.getStaff().getNameEnglish(),
                "sectionId", ct.getSection().getId(),
                "sectionName", ct.getSection().getSectionName(),
                "classId", cls.getId(),
                "className", cls.getName(),
                "academicYearId", ct.getAcademicYear().getId(),
                "academicYearName", ct.getAcademicYear().getYearName()
        );
    }
}
