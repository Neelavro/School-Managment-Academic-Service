package com.example.academic_service.controller;

import com.example.academic_service.entity.StudentGroup;
import com.example.academic_service.service.StudentGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-groups")
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    public StudentGroupController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> migrate(@RequestBody StudentGroup request) {
        studentGroupService.migrateStudentGroup(request);
        return ResponseEntity.ok("Migrated");
    }

    @PostMapping
    public ResponseEntity<StudentGroup> create(@RequestBody StudentGroup group) {
        return ResponseEntity.ok(studentGroupService.createGroup(group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentGroup> update(
            @PathVariable Integer id,
            @RequestBody StudentGroup group
    ) {
        return ResponseEntity.ok(studentGroupService.updateGroup(id, group));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentGroup> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(studentGroupService.getGroupById(id));
    }

    @GetMapping
    public ResponseEntity<List<StudentGroup>> getAll() {
        return ResponseEntity.ok(studentGroupService.getAllActiveGroups());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        studentGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }
}
