package com.example.academic_service.service;

import com.example.academic_service.entity.*;
import com.example.academic_service.entity.Class;
import com.example.academic_service.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Subject teachers: which teacher teaches which subject to a class in a year, without a class routine (LRDM, user
 * decision 2026-10-14). Teacher Duties still assigns routine periods; the teacher portal's mark entry uses these.
 * One teacher per class + subject + gender section + section + group (null = all) in a year.
 */
@Service
@RequiredArgsConstructor
public class TeacherSubjectService {

    private final TeacherSubjectRepository repository;
    private final StaffRepository staffRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final GenderSectionRepository genderSectionRepository;
    private final SectionRepository sectionRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final ExamRoutineRepository examRoutineRepository;

    public List<Map<String, Object>> getByYear(Integer academicYearId) {
        return repository.findByAcademicYear_Id(academicYearId).stream().map(TeacherSubjectService::toMap).toList();
    }

    /** One row per subject: {staffId, academicYearId, classId, subjectIds[], genderSectionId?, sectionId?, groupId?}. */
    @Transactional
    public List<Map<String, Object>> assign(Long staffId, Integer academicYearId, Integer classId, List<Integer> subjectIds,
                                            Integer genderSectionId, Long sectionId, Integer groupId) {
        if (subjectIds == null || subjectIds.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one subject");
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));
        Class cls = classRepository.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));

        Section section = null;
        GenderSection gender = null;
        if (sectionId != null) {
            section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
            if (!Objects.equals(section.getClassEntity().getId(), classId))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That section belongs to another class");
            gender = section.getGenderSection();
        } else if (genderSectionId != null) {
            gender = genderSectionRepository.findById(genderSectionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gender section not found"));
        }
        StudentGroup group = null;
        if (groupId != null) {
            group = studentGroupRepository.findById(groupId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
            if (cls.getStudentGroups().stream().noneMatch(g -> Objects.equals(g.getId(), groupId)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That group is not part of " + cls.getName());
        }

        List<TeacherSubject> saved = new ArrayList<>();
        for (Integer subjectId : new LinkedHashSet<>(subjectIds)) {
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject " + subjectId + " not found"));
            Integer genderId = gender != null ? gender.getId() : null;
            Long secId = section != null ? section.getId() : null;
            Optional<TeacherSubject> taken = repository.findByAcademicYear_IdAndClassEntity_IdAndSubject_Id(academicYearId, classId, subjectId)
                    .stream()
                    .filter(t -> Objects.equals(idOf(t.getGenderSection()), genderId)
                            && Objects.equals(t.getSection() != null ? t.getSection().getId() : null, secId)
                            && Objects.equals(t.getStudentGroup() != null ? t.getStudentGroup().getId() : null, groupId))
                    .findFirst();
            if (taken.isPresent())
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        taken.get().getStaff().getNameEnglish() + " already teaches " + subject.getName() + " here. Remove that first.");

            TeacherSubject ts = new TeacherSubject();
            ts.setStaff(staff);
            ts.setAcademicYear(year);
            ts.setClassEntity(cls);
            ts.setSubject(subject);
            ts.setGenderSection(gender);
            ts.setSection(section);
            ts.setStudentGroup(group);
            saved.add(repository.save(ts));
        }
        return saved.stream().map(TeacherSubjectService::toMap).toList();
    }

    public void remove(Long id) {
        if (!repository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        repository.deleteById(id);
    }

    /** The teacher's subjects in the active academic year (teacher portal mark entry). */
    public List<Map<String, Object>> getMine(Long staffId) {
        return academicYearRepository.findFirstByIsActiveTrue()
                .map(y -> repository.findByStaff_IdAndAcademicYear_Id(staffId, y.getId()).stream().map(TeacherSubjectService::toMap).toList())
                .orElse(List.of());
    }

    /** May this teacher enter marks for the class + subject in this exam routine's academic year? */
    public boolean hasMarkAccess(Long staffId, Integer routineId, Integer classId, Integer subjectId) {
        if (routineId == null || classId == null || subjectId == null) return false;
        return examRoutineRepository.findById(routineId)
                .filter(r -> r.getAcademicYear() != null)
                .map(r -> repository.existsByStaff_IdAndAcademicYear_IdAndClassEntity_IdAndSubject_Id(staffId, r.getAcademicYear().getId(), classId, subjectId))
                .orElse(false);
    }

    private static Integer idOf(GenderSection g) {
        return g != null ? g.getId() : null;
    }

    /** Field names match the old period-based mark assignments, so the teacher portal reads both the same way. */
    private static Map<String, Object> toMap(TeacherSubject t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("staffId", t.getStaff().getId());
        m.put("staffName", t.getStaff().getNameEnglish());
        m.put("academicYearId", t.getAcademicYear().getId());
        m.put("classId", t.getClassEntity().getId());
        m.put("className", t.getClassEntity().getName());
        m.put("shiftName", t.getClassEntity().getShift() != null ? t.getClassEntity().getShift().getName() : null);
        m.put("subjectId", t.getSubject().getId());
        m.put("subjectName", t.getSubject().getName());
        m.put("subjectCode", t.getSubject().getCode());
        m.put("genderSectionId", idOf(t.getGenderSection()));
        m.put("genderSectionName", t.getGenderSection() != null ? t.getGenderSection().getGenderName() : null);
        m.put("sectionId", t.getSection() != null ? t.getSection().getId() : null);
        m.put("sectionName", t.getSection() != null ? t.getSection().getSectionName() : null);
        m.put("groupId", t.getStudentGroup() != null ? t.getStudentGroup().getId() : null);
        m.put("studentGroupName", t.getStudentGroup() != null ? t.getStudentGroup().getGroupName() : null);
        return m;
    }
}
