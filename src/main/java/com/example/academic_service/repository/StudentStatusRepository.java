package com.example.academic_service.repository;

import com.example.academic_service.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentStatusRepository extends JpaRepository<StudentStatus, Integer> {


}
