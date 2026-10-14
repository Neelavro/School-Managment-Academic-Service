package com.example.academic_service.controller;

import com.example.academic_service.config.RequirePermission;
import com.example.academic_service.entity.Submodule;
import com.example.academic_service.service.TeacherSubjectService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Subject teachers (no class routine needed). Uses the Teacher Duties permission. */
@RestController
@RequestMapping("/api/teacher-subjects")
@RequiredArgsConstructor
public class TeacherSubjectController {

    private final TeacherSubjectService service;

    @GetMapping
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "READ")
    public ResponseEntity<ApiResponse> getByYear(@RequestParam Integer academicYearId) {
        return ResponseEntity.ok(new ApiResponse("OK", service.getByYear(academicYearId)));
    }

    /** {staffId, academicYearId, classId, subjectIds[], genderSectionId?, sectionId?, groupId?} */
    @PostMapping
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "CREATE")
    public ResponseEntity<ApiResponse> assign(@RequestBody Map<String, Object> body) {
        Long staffId = body.get("staffId") instanceof Number n ? n.longValue() : null;
        Integer academicYearId = body.get("academicYearId") instanceof Number n ? n.intValue() : null;
        Integer classId = body.get("classId") instanceof Number n ? n.intValue() : null;
        if (staffId == null || academicYearId == null || classId == null)
            return ResponseEntity.badRequest().body(new ApiResponse("Choose the teacher, academic year and class", null));
        List<Integer> subjectIds = body.get("subjectIds") instanceof List<?> l
                ? l.stream().filter(Number.class::isInstance).map(x -> ((Number) x).intValue()).toList()
                : List.of();
        Integer genderSectionId = body.get("genderSectionId") instanceof Number n ? n.intValue() : null;
        Long sectionId = body.get("sectionId") instanceof Number n ? n.longValue() : null;
        Integer groupId = body.get("groupId") instanceof Number n ? n.intValue() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Assigned",
                service.assign(staffId, academicYearId, classId, subjectIds, genderSectionId, sectionId, groupId)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(submodule = Submodule.HR_TEACHER_DUTY, action = "DELETE")
    public ResponseEntity<ApiResponse> remove(@PathVariable Long id) {
        service.remove(id);
        return ResponseEntity.ok(new ApiResponse("Removed", null));
    }
}
