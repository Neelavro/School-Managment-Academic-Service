package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffClassAssignmentService {

    private final StaffClassAssignmentRepository assignmentRepository;
    private final StaffRepository staffRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final GenderSectionRepository genderSectionRepository;
    private final SectionRepository sectionRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final SubjectRepository subjectRepository;

    public List<StaffClassAssignment> getByStaff(Long staffId) {
        return assignmentRepository.findByStaffId(staffId);
    }

    public List<StaffClassAssignment> getByStaffAndYear(Long staffId, Integer academicYearId) {
        return assignmentRepository.findByStaffIdAndAcademicYearId(staffId, academicYearId);
    }

    public List<StaffClassAssignment> getByClassAndYear(Integer classId, Integer academicYearId) {
        return assignmentRepository.findByAcademicYearIdAndAssignedClassId(academicYearId, classId);
    }

    public StaffClassAssignment create(StaffClassAssignment req) {
        StaffClassAssignment a = new StaffClassAssignment();
        a.setStaff(staffRepository.getReferenceById(req.getStaff().getId()));
        a.setAcademicYear(academicYearRepository.getReferenceById(req.getAcademicYear().getId()));
        a.setAssignedClass(classRepository.getReferenceById(req.getAssignedClass().getId()));
        a.setSubject(subjectRepository.getReferenceById(req.getSubject().getId()));
        if (req.getGenderSection() != null && req.getGenderSection().getId() != null)
            a.setGenderSection(genderSectionRepository.getReferenceById(req.getGenderSection().getId()));
        if (req.getSection() != null && req.getSection().getId() != null)
            a.setSection(sectionRepository.getReferenceById(req.getSection().getId()));
        if (req.getStudentGroup() != null && req.getStudentGroup().getId() != null)
            a.setStudentGroup(studentGroupRepository.getReferenceById(req.getStudentGroup().getId()));
        return assignmentRepository.save(a);
    }

    public void delete(Long id) {
        assignmentRepository.deleteById(id);
    }

    @Transactional
    public List<StaffClassAssignment> copyFromYear(Long staffId, Integer fromYearId, Integer toYearId) {
        List<StaffClassAssignment> previous = assignmentRepository.findByStaffIdAndAcademicYearId(staffId, fromYearId);
        AcademicYear toYear = academicYearRepository.getReferenceById(toYearId);
        return previous.stream().map(prev -> {
            StaffClassAssignment copy = new StaffClassAssignment();
            copy.setStaff(prev.getStaff());
            copy.setAcademicYear(toYear);
            copy.setAssignedClass(prev.getAssignedClass());
            copy.setGenderSection(prev.getGenderSection());
            copy.setSection(prev.getSection());
            copy.setStudentGroup(prev.getStudentGroup());
            copy.setSubject(prev.getSubject());
            return assignmentRepository.save(copy);
        }).collect(Collectors.toList());
    }
}
