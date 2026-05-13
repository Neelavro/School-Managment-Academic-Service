package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherOnboardingService {

    private final AcademicQualificationRepository qualificationRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final StaffDependentRepository dependentRepository;
    private final StaffRepository staffRepository;
    private final EmploymentHistoryRepository employmentHistoryRepository;

    // ── Academic Qualifications ───────────────────────────────────────────────

    public List<AcademicQualification> getQualifications(Long staffId) {
        return qualificationRepository.findByStaffIdOrderByLevel(staffId);
    }

    public AcademicQualification saveQualification(Long staffId, AcademicQualification req) {
        req.setStaff(staffRepository.getReferenceById(staffId));
        return qualificationRepository.save(req);
    }

    public void deleteQualification(Long qualificationId) {
        qualificationRepository.deleteById(qualificationId);
    }

    @Transactional
    public List<AcademicQualification> replaceQualifications(Long staffId, List<AcademicQualification> list) {
        qualificationRepository.deleteByStaffId(staffId);
        Staff ref = staffRepository.getReferenceById(staffId);
        list.forEach(q -> q.setStaff(ref));
        return qualificationRepository.saveAll(list);
    }

    // ── Teacher Profile (MPO, NTRCA, experience) ─────────────────────────────

    public TeacherProfile getProfile(Long staffId) {
        return teacherProfileRepository.findByStaffId(staffId)
                .orElse(null);
    }

    @Transactional
    public TeacherProfile saveProfile(Long staffId, TeacherProfile req) {
        TeacherProfile profile = teacherProfileRepository.findByStaffId(staffId)
                .orElse(new TeacherProfile());
        profile.setStaff(staffRepository.getReferenceById(staffId));
        profile.setMpoStatus(req.getMpoStatus());
        profile.setMpoIndexNumber(req.getMpoIndexNumber());
        profile.setNtrcaRegNumber(req.getNtrcaRegNumber());
        profile.setNtrcaCycle(req.getNtrcaCycle());
        profile.setYearsOfExperience(req.getYearsOfExperience());
        profile.setPreviousInstitutions(req.getPreviousInstitutions());
        profile.setTrainingsCompleted(req.getTrainingsCompleted());
        return teacherProfileRepository.save(profile);
    }

    // ── Employment History ────────────────────────────────────────────────────

    public List<EmploymentHistory> getEmploymentHistory(Long staffId) {
        return employmentHistoryRepository.findByStaffIdOrderByFromDateDesc(staffId);
    }

    @Transactional
    public List<EmploymentHistory> replaceEmploymentHistory(Long staffId, List<EmploymentHistory> list) {
        employmentHistoryRepository.deleteByStaffId(staffId);
        Staff ref = staffRepository.getReferenceById(staffId);
        list.forEach(h -> h.setStaff(ref));
        return employmentHistoryRepository.saveAll(list);
    }

    // ── Dependents (spouse, children) ────────────────────────────────────────

    public List<StaffDependent> getDependents(Long staffId) {
        return dependentRepository.findByStaffId(staffId);
    }

    public StaffDependent addDependent(Long staffId, StaffDependent req) {
        req.setStaff(staffRepository.getReferenceById(staffId));
        return dependentRepository.save(req);
    }

    public void deleteDependent(Long dependentId) {
        dependentRepository.deleteById(dependentId);
    }

    @Transactional
    public List<StaffDependent> replaceDependents(Long staffId, List<StaffDependent> list) {
        dependentRepository.deleteByStaffId(staffId);
        Staff ref = staffRepository.getReferenceById(staffId);
        list.forEach(d -> d.setStaff(ref));
        return dependentRepository.saveAll(list);
    }
}
