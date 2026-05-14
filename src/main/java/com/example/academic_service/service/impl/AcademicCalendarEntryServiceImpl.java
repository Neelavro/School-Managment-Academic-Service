package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.AcademicCalendarEntryRequestDto;
import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.CalendarEntryType;
import com.example.academic_service.repository.AcademicCalendarEntryRepository;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.service.AcademicCalendarEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicCalendarEntryServiceImpl implements AcademicCalendarEntryService {

    private final AcademicCalendarEntryRepository repository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    public ApiResponse<AcademicCalendarEntry> create(AcademicCalendarEntryRequestDto dto) {
        AcademicYear year = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
        if (year == null) return ApiResponse.error("Academic year not found");

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            return ApiResponse.error("End date must be on or after start date");
        }

        if (dto.getType() == CalendarEntryType.RAMADAN &&
                repository.existsByAcademicYear_IdAndTypeAndIsActiveTrue(dto.getAcademicYearId(), CalendarEntryType.RAMADAN)) {
            return ApiResponse.error("A Ramadan period is already defined for this academic year. Please edit or delete the existing one.");
        }

        AcademicCalendarEntry entry = new AcademicCalendarEntry();
        entry.setAcademicYear(year);
        entry.setType(dto.getType());
        entry.setName(dto.getName());
        entry.setStartDate(dto.getStartDate());
        entry.setEndDate(dto.getEndDate());
        return ApiResponse.success("Calendar entry created successfully", repository.save(entry));
    }

    @Override
    public ApiResponse<AcademicCalendarEntry> update(Integer id, AcademicCalendarEntryRequestDto dto) {
        AcademicCalendarEntry entry = repository.findById(id).orElse(null);
        if (entry == null) return ApiResponse.error("Calendar entry not found");
        if (!entry.getIsActive()) return ApiResponse.error("Cannot update an inactive entry");

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            return ApiResponse.error("End date must be on or after start date");
        }

        if (dto.getType() == CalendarEntryType.RAMADAN) {
            AcademicCalendarEntry existing = repository
                    .findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(dto.getAcademicYearId(), CalendarEntryType.RAMADAN)
                    .stream().filter(e -> !e.getId().equals(id)).findFirst().orElse(null);
            if (existing != null) {
                return ApiResponse.error("A Ramadan period is already defined for this academic year.");
            }
        }

        AcademicYear year = academicYearRepository.findById(dto.getAcademicYearId()).orElse(null);
        if (year == null) return ApiResponse.error("Academic year not found");

        entry.setAcademicYear(year);
        entry.setType(dto.getType());
        entry.setName(dto.getName());
        entry.setStartDate(dto.getStartDate());
        entry.setEndDate(dto.getEndDate());
        return ApiResponse.success("Calendar entry updated successfully", repository.save(entry));
    }

    @Override
    public ApiResponse<List<AcademicCalendarEntry>> getByAcademicYear(Integer academicYearId, CalendarEntryType type) {
        List<AcademicCalendarEntry> entries;
        if (type != null) {
            entries = repository.findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(academicYearId, type);
        } else {
            entries = repository.findByAcademicYear_IdAndIsActiveTrueOrderByStartDateAsc(academicYearId);
        }
        return ApiResponse.success("Calendar entries fetched successfully", entries);
    }

    @Override
    public ApiResponse<Void> delete(Integer id) {
        AcademicCalendarEntry entry = repository.findById(id).orElse(null);
        if (entry == null) return ApiResponse.error("Calendar entry not found");
        if (!entry.getIsActive()) return ApiResponse.error("Entry is already inactive");
        entry.setIsActive(false);
        repository.save(entry);
        return ApiResponse.success("Calendar entry deleted successfully", null);
    }
}
