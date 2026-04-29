package com.example.academic_service.service;

import com.example.academic_service.entity.Student;
import com.example.academic_service.entity.StudentStatus;

import java.util.List;

public interface StudentService {
    Student createStudent(Student student);
    Student getStudentById(Long id);
    Student getStudentBySystemId(String studentSystemId);
    List<StudentStatus> getStudentStatus();
    List<Student> getAllStudents();
    Student updateStudent(Long id, Student student);
    Student updateStudentBySystemId(String studentSystemId, Student student);
    Student migrateStudent(Long id, Student student);
    void deleteStudent(Long id);
}
