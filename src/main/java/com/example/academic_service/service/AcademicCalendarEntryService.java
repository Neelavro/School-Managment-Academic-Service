package com.example.academic_service.service;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.AcademicCalendarEntryRequestDto;
import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.CalendarEntryType;

import java.util.List;

public interface AcademicCalendarEntryService {
    ApiResponse<AcademicCalendarEntry> create(AcademicCalendarEntryRequestDto dto);
    ApiResponse<AcademicCalendarEntry> update(Integer id, AcademicCalendarEntryRequestDto dto);
    ApiResponse<List<AcademicCalendarEntry>> getByAcademicYear(Integer academicYearId, CalendarEntryType type);
    ApiResponse<Void> delete(Integer id);
}
