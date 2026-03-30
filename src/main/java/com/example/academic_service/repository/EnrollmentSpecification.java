package com.example.academic_service.repository;

import com.example.academic_service.entity.Enrollment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentSpecification {

    public static Specification<Enrollment> filter(
            Integer academicYearId,
            Integer classId,
            Long sectionId,
            Integer shiftId,
            Integer genderSectionId,
            Integer studentGroupId,
            Boolean isActive
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (academicYearId != null) {
                predicates.add(cb.equal(root.get("academicYear").get("id"), academicYearId));
            }
            if (classId != null) {
                predicates.add(cb.equal(root.get("studentClass").get("id"), classId));
            }
            if (sectionId != null) {
                predicates.add(cb.equal(root.get("section").get("id"), sectionId));
            }
            if (shiftId != null) {
                predicates.add(cb.equal(root.get("shift").get("id"), shiftId));
            }
            if (genderSectionId != null) {
                predicates.add(cb.equal(root.get("genderSection").get("id"), genderSectionId));
            }
            if (studentGroupId != null) {
                predicates.add(cb.equal(root.get("studentGroup").get("id"), studentGroupId));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}