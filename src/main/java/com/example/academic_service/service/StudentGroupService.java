package com.example.academic_service.service;

import com.example.academic_service.entity.StudentGroup;

import java.util.List;

public interface StudentGroupService {

    public void migrateStudentGroup(StudentGroup request);

    StudentGroup createGroup(StudentGroup group);

    StudentGroup updateGroup(Integer id, StudentGroup group);

    StudentGroup getGroupById(Integer id);

    List<StudentGroup> getAllActiveGroups();

    void deleteGroup(Integer id); // soft delete
}
