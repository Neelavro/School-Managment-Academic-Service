package com.example.academic_service.controller;

import com.example.academic_service.entity.Student;
import com.example.academic_service.service.StudentService;
import com.example.academic_service.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createStudent(@RequestBody Student student) {
        Map<String, Object> response = new HashMap<>();
        try {
            Student created = studentService.createStudent(student);
            response.put("success", true);
            response.put("message", "Student created successfully");
            response.put("id", created.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Could not create student: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudent(@PathVariable Long id) {
        Student student = studentService.getStudentById(id);
        if (student == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(student);
    }

    @GetMapping("/system/{studentSystemId}")
    public ResponseEntity<Student> getStudentBySystemId(@PathVariable String studentSystemId) {
        Student student = studentService.getStudentBySystemId(studentSystemId);
        if (student == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(student);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Student>>> getAllStudents() {
        try {
            List<Student> students = studentService.getAllStudents();
            return ResponseEntity.ok(new ApiResponse<>("Students retrieved successfully", students, true));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>("Could not retrieve students: " + ex.getMessage(), null, false));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateStudent(
            @PathVariable Long id,
            @RequestBody Student student
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            Student updated = studentService.updateStudent(id, student);
            if (updated == null) {
                response.put("success", false);
                response.put("message", "Student not found");
                return ResponseEntity.status(404).body(response);
            }
            response.put("success", true);
            response.put("message", "Student updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Could not update student: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/by-system-id/{studentSystemId}")
    public ResponseEntity<Map<String, Object>> updateStudentBySystemId(
            @PathVariable String studentSystemId,
            @RequestBody Student student
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            Student updated = studentService.updateStudentBySystemId(studentSystemId, student);
            if (updated == null) {
                response.put("success", false);
                response.put("message", "Student not found with systemId: " + studentSystemId);
                return ResponseEntity.status(404).body(response);
            }
            response.put("success", true);
            response.put("message", "Student updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Could not update student: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/migrate/{id}")
    public ResponseEntity<Map<String, Object>> migrateStudent(
            @PathVariable Long id,
            @RequestBody Student student
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            Student updated = studentService.migrateStudent(id, student);
            if (updated == null) {
                response.put("success", false);
                response.put("message", "Student not found");
                return ResponseEntity.status(404).body(response);
            }
            response.put("success", true);
            response.put("message", "Student migrated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Could not migrate student: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);
            return ResponseEntity.ok(new ApiResponse<>("Student deleted successfully", null, true));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>("Could not delete student: " + ex.getMessage(), null, false));
        }
    }
}
