package com.example.academic_service.repository;

import com.example.academic_service.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findByIsActiveTrue();
    List<Room> findByIsActiveFalse();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
    // for reactivation duplicate check
    boolean existsByNameIgnoreCaseAndIsActiveTrueAndIdNot(String name, Integer id);
}