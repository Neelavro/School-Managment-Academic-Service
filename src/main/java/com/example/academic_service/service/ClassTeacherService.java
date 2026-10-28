package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class teachers. A register is a section, or — when the class has no sections for that gender section — the
 * class + gender section (user decision 2026-09-30). Where sections exist, one must be picked. Each register has one
 * class teacher per year; a teacher may keep several registers (user, 2026-10-18).
 */
@Service
@RequiredArgsConstructor
public class ClassTeacherService {

    private final ClassTeacherRepository classTeacherRepository;
    private final StaffRepository staffRepository;
    private final SectionRepository sectionRepository;
    private final ClassRepository classRepository;
    private final GenderSectionRepository genderSectionRepository;
    private final AcademicYearRepository academicYearRepository;

    public List<Map<String, Object>> getByYear(Integer academicYearId) {
        return classTeacherRepository.findAllByAcademicYearId(academicYearId)
                .stream().map(this::toMap).toList();
    }

    /** sectionId, or classId + genderSectionId for a class/gender section without sections. */
    public Map<String, Object> assign(Long staffId, Long sectionId, Integer classId, Integer genderSectionId, Integer academicYearId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));

        ClassTeacher ct = new ClassTeacher();
        setRegister(ct, sectionId, classId, genderSectionId, academicYearId, null);
        ct.setStaff(staff);
        ct.setAcademicYear(year);
        return toMap(classTeacherRepository.save(ct));
    }

    /**
     * Edit an assignment: change the teacher and/or the register (section, or class + gender section).
     * The academic year stays the same.
     */
    public Map<String, Object> update(Long id, Long staffId, Long sectionId, Integer classId, Integer genderSectionId) {
        ClassTeacher ct = classTeacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        Integer academicYearId = ct.getAcademicYear().getId();
        ct.setSection(null);
        ct.setStudentClass(null);
        ct.setGenderSection(null);
        setRegister(ct, sectionId, classId, genderSectionId, academicYearId, id);
        ct.setStaff(staff);
        return toMap(classTeacherRepository.save(ct));
    }

    /** Points ct at a register after checking it exists and has no other class teacher (excludeId = the row being edited). */
    private void setRegister(ClassTeacher ct, Long sectionId, Integer classId, Integer genderSectionId,
                             Integer academicYearId, Long excludeId) {
        if (sectionId != null) {
            if (classTeacherRepository.findBySectionIdAndAcademicYearId(sectionId, academicYearId)
                    .filter(other -> !other.getId().equals(excludeId)).isPresent())
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This section already has a class teacher for this academic year");
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
            ct.setSection(section);
            ct.setStudentClass(section.getClassEntity());
            ct.setGenderSection(section.getGenderSection());
        } else {
            if (classId == null || genderSectionId == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a class and a gender section");
            if (!sectionRepository.findAllByClassEntityIdAndGenderSectionIdAndIsActiveTrue(classId, genderSectionId).isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This class has sections for this gender section, so choose a section");
            if (classTeacherRepository.findAllByStudentClassIdAndGenderSectionIdAndSectionIsNullAndAcademicYearId(classId, genderSectionId, academicYearId)
                    .stream().anyMatch(other -> !other.getId().equals(excludeId)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This class and gender section already have a class teacher for this academic year");
            ct.setStudentClass(classRepository.findById(classId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found")));
            ct.setGenderSection(genderSectionRepository.findById(genderSectionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gender section not found")));
        }
    }

    public void remove(Long id) {
        if (!classTeacherRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        classTeacherRepository.deleteById(id);
    }

    // Used by teacher portal & attendance controller. A teacher can keep several registers in a year.
    public List<ClassTeacher> getAllByStaffAndYear(Long staffId, Integer academicYearId) {
        return classTeacherRepository.findAllByStaffIdAndAcademicYearIdOrderByIdAsc(staffId, academicYearId);
    }

    /**
     * One of the teacher's registers: the given assignment (must be theirs), or the first one when none is given
     * (older clients that only know one class).
     */
    public ClassTeacher getByStaffAndYear(Long staffId, Integer academicYearId, Long classTeacherId) {
        List<ClassTeacher> mine = getAllByStaffAndYear(staffId, academicYearId);
        if (mine.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "You are not assigned as a class teacher for this academic year");
        if (classTeacherId == null) return mine.get(0);
        return mine.stream().filter(ct -> ct.getId().equals(classTeacherId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not the class teacher of that class"));
    }

    /** The first register only — kept for older clients. */
    public Map<String, Object> getMyAssignment(Long staffId, Integer academicYearId) {
        return toMap(getByStaffAndYear(staffId, academicYearId, null));
    }

    public List<Map<String, Object>> getMyAssignments(Long staffId, Integer academicYearId) {
        return getAllByStaffAndYear(staffId, academicYearId).stream().map(this::toMap).toList();
    }

    /** sectionId / sectionName are null for a class + gender section register. */
    private Map<String, Object> toMap(ClassTeacher ct) {
        Class cls = ct.getStudentClass() != null ? ct.getStudentClass()
                : ct.getSection() != null ? ct.getSection().getClassEntity() : null;
        GenderSection gs = ct.getGenderSection() != null ? ct.getGenderSection()
                : ct.getSection() != null ? ct.getSection().getGenderSection() : null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ct.getId());
        m.put("staffId", ct.getStaff().getId());
        m.put("staffName", ct.getStaff().getNameEnglish());
        m.put("sectionId", ct.getSection() != null ? ct.getSection().getId() : null);
        m.put("sectionName", ct.getSection() != null ? ct.getSection().getSectionName() : null);
        m.put("classId", cls != null ? cls.getId() : null);
        m.put("className", cls != null ? cls.getName() : null);
        m.put("genderSectionId", gs != null ? gs.getId() : null);
        m.put("genderSectionName", gs != null ? gs.getGenderName() : null);
        m.put("academicYearId", ct.getAcademicYear().getId());
        m.put("academicYearName", ct.getAcademicYear().getYearName());
        return m;
    }
}
