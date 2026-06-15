package com.example.academic_service.service.impl;

import com.example.academic_service.entity.Gender;
import com.example.academic_service.entity.Student;
import com.example.academic_service.entity.StudentStatus;
import com.example.academic_service.repository.GenderRepository;
import com.example.academic_service.repository.StudentRepository;
import com.example.academic_service.repository.StudentStatusRepository;
import com.example.academic_service.entity.AuditActionType;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.AuditLogService;
import com.example.academic_service.service.StudentService;
import com.example.academic_service.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final GenderRepository genderRepository;
    private final StudentStatusRepository studentStatusRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private String generateStudentSystemId() {
        String yearPrefix = String.valueOf(java.time.Year.now().getValue());
        String maxId = studentRepository.findMaxStudentSystemIdByYear(yearPrefix);

        int nextNumber;
        if (maxId == null || maxId.isEmpty()) {
            nextNumber = 1;
        } else {
            String lastFourDigits = maxId.substring(4);
            nextNumber = Integer.parseInt(lastFourDigits) + 1;
        }

        return yearPrefix + String.format("%04d", nextNumber);
    }

    private void assignOrUpdateStudentSystemId(Student student, Student existing) {
        String newId = student.getStudentSystemId();
        if (newId == null || newId.isBlank()) {
            existing.setStudentSystemId(generateStudentSystemId());
        } else if (!newId.equals(existing.getStudentSystemId())) {
            if (studentRepository.existsByStudentSystemId(newId)) {
                throw new IllegalArgumentException("studentSystemId already exists: " + newId);
            }
            existing.setStudentSystemId(newId);
        }
    }

    @Override
    public Student createStudent(Student student) {
        student.setIsActive(true);
        student.setPasswordHash(passwordEncoder.encode("123456"));
        assignOrUpdateStudentSystemId(student, student);

        if (student.getGender() != null) {
            student.setGender(genderRepository.getReferenceById(student.getGender().getId()));
        }
        if (student.getStudentStatus() != null) {
            student.setStudentStatus(studentStatusRepository.getReferenceById(student.getStudentStatus().getId()));
        }

        Student saved = studentRepository.save(student);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.CREATE, Submodule.STUDENTS, "Student", saved.getId().toString(),
            "Created student: " + saved.getNameEnglish() + " (" + saved.getStudentSystemId() + ")",
            null, AuditHelper.toJson(studentSnapshot(saved)));
        return saved;
    }

    @Override
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .filter(Student::getIsActive)
                .orElse(null);
    }

    @Override
    public Student getStudentBySystemId(String studentSystemId) {
        return studentRepository.findByStudentSystemId(studentSystemId)
                .filter(Student::getIsActive)
                .orElse(null);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .filter(Student::getIsActive)
                .toList();
    }

    @Override
    public Student updateStudent(Long id, Student student) {
        return studentRepository.findById(id)
                .map(existing -> {
                    String before = AuditHelper.toJson(studentSnapshot(existing));
                    mapSimpleFields(student, existing);
                    assignOrUpdateStudentSystemId(student, existing);

                    if (student.getGender() != null) {
                        existing.setGender(genderRepository.getReferenceById(student.getGender().getId()));
                    }
                    if (student.getStudentStatus() != null) {
                        existing.setStudentStatus(studentStatusRepository.getReferenceById(student.getStudentStatus().getId()));
                    }

                    Student saved = studentRepository.save(existing);
                    auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
                        AuditActionType.UPDATE, Submodule.STUDENTS, "Student", id.toString(),
                        "Updated student: " + saved.getNameEnglish() + " (" + saved.getStudentSystemId() + ")",
                        before, AuditHelper.toJson(studentSnapshot(saved)));
                    return saved;
                })
                .orElse(null);
    }

    @Override
    public Student updateStudentBySystemId(String studentSystemId, Student student) {
        return studentRepository.findByStudentSystemId(studentSystemId)
                .map(existing -> {
                    mapSimpleFields(student, existing);

                    if (student.getGender() != null) {
                        existing.setGender(genderRepository.getReferenceById(student.getGender().getId()));
                    }
                    if (student.getStudentStatus() != null) {
                        existing.setStudentStatus(studentStatusRepository.getReferenceById(student.getStudentStatus().getId()));
                    }

                    return studentRepository.save(existing);
                })
                .orElse(null);
    }

    @Override
    public Student migrateStudent(Long id, Student student) {
        return studentRepository.findById(id)
                .map(existing -> {
                    mapSimpleFields(student, existing);

                    existing.setGender(
                            student.getGender() != null
                                    ? genderRepository.getReferenceById(student.getGender().getId())
                                    : null
                    );
                    existing.setStudentStatus(
                            student.getStudentStatus() != null
                                    ? studentStatusRepository.getReferenceById(student.getStudentStatus().getId())
                                    : null
                    );
                    existing.setImage(student.getImage());

                    return studentRepository.save(existing);
                })
                .orElse(null);
    }

    @Override
    public List<StudentStatus> getStudentStatus() {
        return studentStatusRepository.findAll();
    }

    @Override
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        String before = AuditHelper.toJson(studentSnapshot(student));
        student.setIsActive(false);
        studentRepository.save(student);
        auditLogService.log(AuditHelper.getUserId(), AuditHelper.getIp(),
            AuditActionType.DELETE, Submodule.STUDENTS, "Student", id.toString(),
            "Deactivated student: " + student.getNameEnglish() + " (" + student.getStudentSystemId() + ")",
            before, null);
    }

    private void mapSimpleFields(Student src, Student dest) {
        if (src.getNameBangla()             != null) dest.setNameBangla(src.getNameBangla());
        if (src.getNameEnglish()            != null) dest.setNameEnglish(src.getNameEnglish());
        if (src.getFatherNameBangla()       != null) dest.setFatherNameBangla(src.getFatherNameBangla());
        if (src.getFatherNameEnglish()      != null) dest.setFatherNameEnglish(src.getFatherNameEnglish());
        if (src.getFatherOccupation()       != null) dest.setFatherOccupation(src.getFatherOccupation());
        if (src.getFatherPhone()            != null) dest.setFatherPhone(src.getFatherPhone());
        if (src.getFatherMonthlySalary()    != null) dest.setFatherMonthlySalary(src.getFatherMonthlySalary());
        if (src.getMotherNameBangla()       != null) dest.setMotherNameBangla(src.getMotherNameBangla());
        if (src.getMotherNameEnglish()      != null) dest.setMotherNameEnglish(src.getMotherNameEnglish());
        if (src.getMotherOccupation()       != null) dest.setMotherOccupation(src.getMotherOccupation());
        if (src.getMotherPhone()            != null) dest.setMotherPhone(src.getMotherPhone());
        if (src.getMotherMonthlySalary()    != null) dest.setMotherMonthlySalary(src.getMotherMonthlySalary());
        if (src.getGuardianNameBangla()     != null) dest.setGuardianNameBangla(src.getGuardianNameBangla());
        if (src.getGuardianNameEnglish()    != null) dest.setGuardianNameEnglish(src.getGuardianNameEnglish());
        if (src.getGuardianOccupation()     != null) dest.setGuardianOccupation(src.getGuardianOccupation());
        if (src.getGuardianPhone()          != null) dest.setGuardianPhone(src.getGuardianPhone());
        if (src.getGuardianRelation()       != null) dest.setGuardianRelation(src.getGuardianRelation());
        if (src.getCurrentHoldingNo()       != null) dest.setCurrentHoldingNo(src.getCurrentHoldingNo());
        if (src.getCurrentRoadOrVillage()   != null) dest.setCurrentRoadOrVillage(src.getCurrentRoadOrVillage());
        if (src.getCurrentDistrict()        != null) dest.setCurrentDistrict(src.getCurrentDistrict());
        if (src.getCurrentThana()           != null) dest.setCurrentThana(src.getCurrentThana());
        if (src.getPermanentHoldingNo()     != null) dest.setPermanentHoldingNo(src.getPermanentHoldingNo());
        if (src.getPermanentRoadOrVillage() != null) dest.setPermanentRoadOrVillage(src.getPermanentRoadOrVillage());
        if (src.getPermanentDistrict()      != null) dest.setPermanentDistrict(src.getPermanentDistrict());
        if (src.getPermanentThana()         != null) dest.setPermanentThana(src.getPermanentThana());
        if (src.getDob()                    != null) dest.setDob(src.getDob());
        if (src.getNationality()            != null) dest.setNationality(src.getNationality());
        if (src.getClassRoll()              != null) dest.setClassRoll(src.getClassRoll());
        dest.setIsActive(true);
    }

    private Map<String, Object> studentSnapshot(Student s) {
        return Map.of(
            "id", s.getId() != null ? s.getId() : "",
            "studentSystemId", s.getStudentSystemId() != null ? s.getStudentSystemId() : "",
            "nameEnglish", s.getNameEnglish() != null ? s.getNameEnglish() : "",
            "nameBangla", s.getNameBangla() != null ? s.getNameBangla() : "",
            "fatherPhone", s.getFatherPhone() != null ? s.getFatherPhone() : "",
            "motherPhone", s.getMotherPhone() != null ? s.getMotherPhone() : "",
            "dob", s.getDob() != null ? s.getDob().toString() : "",
            "isActive", s.getIsActive() != null ? s.getIsActive() : false
        );
    }
}
