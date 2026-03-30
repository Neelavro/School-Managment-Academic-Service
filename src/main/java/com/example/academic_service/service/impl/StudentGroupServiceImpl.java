package com.example.academic_service.service.impl;

import com.example.academic_service.entity.StudentGroup;
import com.example.academic_service.repository.StudentGroupRepository;
import com.example.academic_service.service.StudentGroupService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {

    private final StudentGroupRepository groupRepository;

    public StudentGroupServiceImpl(StudentGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public StudentGroup createGroup(StudentGroup group) {
        group.setIsActive(true);
        return groupRepository.save(group);
    }
    @PersistenceContext
    private EntityManager entityManager;
    @Override
    @Transactional
    public void migrateStudentGroup(StudentGroup request) {
        entityManager.createNativeQuery(
                        "INSERT INTO student_group (id, group_name, is_active) VALUES (:id, :groupName, :isActive)"
                )
                .setParameter("id", request.getId())
                .setParameter("groupName", request.getGroupName())
                .setParameter("isActive", request.getIsActive())
                .executeUpdate();
    }

    @Override
    public StudentGroup updateGroup(Integer id, StudentGroup group) {
        StudentGroup existing = getGroupById(id);
        existing.setGroupName(group.getGroupName());
        existing.setIsActive(group.getIsActive());
        return groupRepository.save(existing);
    }

    @Override
    public StudentGroup getGroupById(Integer id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student group not found"));
    }

    @Override
    public List<StudentGroup> getAllActiveGroups() {
        return groupRepository.findAllByIsActiveTrue();
    }

    @Override
    public void deleteGroup(Integer id) {
        StudentGroup group = getGroupById(id);
        group.setIsActive(false); // soft delete
        groupRepository.save(group);
    }
}
