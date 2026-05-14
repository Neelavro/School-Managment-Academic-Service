package com.example.academic_service.repository;

import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.CalendarEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicCalendarEntryRepository extends JpaRepository<AcademicCalendarEntry, Integer> {

    List<AcademicCalendarEntry> findByAcademicYear_IdAndIsActiveTrueOrderByStartDateAsc(Integer academicYearId);

    List<AcademicCalendarEntry> findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(
            Integer academicYearId, CalendarEntryType type);

    boolean existsByAcademicYear_IdAndTypeAndIsActiveTrue(Integer academicYearId, CalendarEntryType type);
}
