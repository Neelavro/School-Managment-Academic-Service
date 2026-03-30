package com.example.academic_service.controller;

import com.example.academic_service.dto.ReorderClassesRequest;
import com.example.academic_service.entity.Class;
import com.example.academic_service.service.ClassService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @PostMapping("/migrate")
    public ResponseEntity<?> migrate(@RequestBody Class request) {
        classService.migrateClass(request);
        return ResponseEntity.ok("Migrated");
    }
    @PostMapping
    public ResponseEntity<Class> createClass(@RequestBody Class clazz) {
        return ResponseEntity.ok(classService.createClass(clazz));
    }
    @GetMapping()
    public ResponseEntity<List<Class>> getAllClasses() {
        List<Class> classes = classService.getAllClasses();
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<Class>> getAllClassesById(@PathVariable Integer id) {
        List<Class> classes = classService.getAllClassesById(id);
        return ResponseEntity.ok(classes);
    }


    @GetMapping("/only/{id}")
    public ResponseEntity<Class> getClassById(@PathVariable Integer id) {
        Class clazz = classService.getClassById(id);
        if (clazz == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(clazz);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Class> updateClass(
            @PathVariable Integer id,
            @RequestBody Class clazz
    ) {
        Class updated = classService.updateClass(id, clazz);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Integer id) {
        classService.deleteClass(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/reorder")
    public ResponseEntity<List<Class>> reorderClasses(@RequestBody ReorderClassesRequest request) {
        return ResponseEntity.ok(classService.reorderClasses(request));
    }
}
