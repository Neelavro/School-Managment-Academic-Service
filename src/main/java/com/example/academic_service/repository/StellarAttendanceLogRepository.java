package com.example.academic_service.repository;

import com.example.academic_service.entity.StellarAttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface StellarAttendanceLogRepository extends JpaRepository<StellarAttendanceLog, Long> {

    long countByAccessDate(LocalDate accessDate);

    @Query("select count(distinct l.registrationId) from StellarAttendanceLog l " +
           "where l.accessDate = :date and l.registrationId is not null")
    long countDistinctRegistrationIdsByDate(@Param("date") LocalDate date);
}
