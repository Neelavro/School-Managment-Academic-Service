package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.schedule_dtos.AcademicCalendarEntryRequestDto;
import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.CalendarEntryType;
import com.example.academic_service.entity.ExamRoutine;
import com.example.academic_service.repository.AcademicCalendarEntryRepository;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.ExamRoutineRepository;
import com.example.academic_service.service.AcademicCalendarEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AcademicCalendarEntryServiceImpl implements AcademicCalendarEntryService {

    private final AcademicCalendarEntryRepository repository;
    private final AcademicYearRepository academicYearRepository;
    private final ExamRoutineRepository examRoutineRepository;

    /**
     * Exam Days take their name from the exam routine they are for (user, 2026-11-03); other types need a name.
     * Returns the name to store. Errors are HTTP 400 (an ApiResponse.error is a 200 the panel cannot tell apart).
     */
    private String resolveName(AcademicCalendarEntryRequestDto dto, Integer excludeId) {
        if (dto.getType() == CalendarEntryType.EXAM_DAY) {
            if (dto.getExamRoutineId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose the exam these dates are for");
            ExamRoutine routine = examRoutineRepository.findById(dto.getExamRoutineId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam routine not found"));
            if (routine.getAcademicYear() == null || !Objects.equals(routine.getAcademicYear().getId(), dto.getAcademicYearId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That exam belongs to another academic year");
            }
            boolean taken = repository.findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(dto.getAcademicYearId(), CalendarEntryType.EXAM_DAY)
                    .stream().anyMatch(e -> Objects.equals(e.getExamRoutineId(), routine.getId()) && !e.getId().equals(excludeId));
            if (taken) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, routine.getTitle() + " already has exam days; edit those instead");
            return routine.getTitle();
        }
        if (dto.getName() == null || dto.getName().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        return dto.getName().trim();
    }

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

        String name = resolveName(dto, null);
        AcademicCalendarEntry entry = new AcademicCalendarEntry();
        entry.setAcademicYear(year);
        entry.setType(dto.getType());
        entry.setName(name);
        entry.setExamRoutineId(dto.getType() == CalendarEntryType.EXAM_DAY ? dto.getExamRoutineId() : null);
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

        String name = resolveName(dto, id);
        entry.setAcademicYear(year);
        entry.setType(dto.getType());
        entry.setName(name);
        entry.setExamRoutineId(dto.getType() == CalendarEntryType.EXAM_DAY ? dto.getExamRoutineId() : null);
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

    /**
     * Copies a year's Exam Days into another year, every date moved one year later (user, 2026-11-03). Each copy is
     * linked to the target year's exam routine with the same title; an exam that already has Exam Days there, or
     * has no routine of that name, is skipped and named in the result.
     */
    @Override
    @Transactional
    public Map<String, Object> copyExamDays(Integer fromYearId, Integer toYearId) {
        if (fromYearId == null || toYearId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose both years");
        if (fromYearId.equals(toYearId)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose two different years");
        AcademicYear target = academicYearRepository.findById(toYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Academic year not found"));
        Map<String, ExamRoutine> routines = new HashMap<>();
        for (ExamRoutine r : examRoutineRepository.findByAcademicYearIdAndIsActiveTrue(toYearId)) {
            if (r.getTitle() != null) routines.putIfAbsent(key(r.getTitle()), r);
        }
        Set<Integer> haveDays = new HashSet<>();
        for (AcademicCalendarEntry e : repository.findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(toYearId, CalendarEntryType.EXAM_DAY)) {
            if (e.getExamRoutineId() != null) haveDays.add(e.getExamRoutineId());
        }
        List<String> copied = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (AcademicCalendarEntry e : repository.findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(fromYearId, CalendarEntryType.EXAM_DAY)) {
            ExamRoutine routine = routines.get(key(e.getName()));
            if (routine == null) {
                skipped.add(e.getName() + " (no exam routine with this name in the new year)");
                continue;
            }
            if (haveDays.contains(routine.getId())) {
                skipped.add(e.getName() + " (already has exam days)");
                continue;
            }
            AcademicCalendarEntry copy = new AcademicCalendarEntry();
            copy.setAcademicYear(target);
            copy.setType(CalendarEntryType.EXAM_DAY);
            copy.setName(routine.getTitle());
            copy.setExamRoutineId(routine.getId());
            copy.setStartDate(e.getStartDate().plusYears(1));
            copy.setEndDate(e.getEndDate().plusYears(1));
            repository.save(copy);
            haveDays.add(routine.getId());
            copied.add(routine.getTitle());
        }
        return Map.of("copied", copied, "skipped", skipped);
    }

    private static String key(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toUpperCase();
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
