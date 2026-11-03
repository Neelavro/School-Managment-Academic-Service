package com.example.academic_service.service;

import com.example.academic_service.entity.AcademicCalendarEntry;
import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.entity.CalendarEntryType;
import com.example.academic_service.entity.SystemSettings;
import com.example.academic_service.repository.AcademicCalendarEntryRepository;
import com.example.academic_service.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The school calendar's shared rules (user, 2026-11-03): an academic year's first and last day, the school's weekly
 * off days, and the class days between two dates (not a weekend, not inside an active Holiday entry).
 */
@Service
@RequiredArgsConstructor
public class SchoolCalendarService {

    /** Used while the school has not chosen its weekends (most Bangladeshi schools close on Friday). */
    public static final List<DayOfWeek> DEFAULT_WEEKENDS = List.of(DayOfWeek.FRIDAY);

    private final SystemSettingsService systemSettingsService;
    private final SystemSettingsRepository systemSettingsRepository;
    private final AcademicCalendarEntryRepository calendarRepository;

    /** First day of the year: its start date, else 1 January of the year in its name ("2026"), else null. */
    public static LocalDate yearStart(AcademicYear year) {
        if (year == null) return null;
        if (year.getStartDate() != null) return year.getStartDate();
        String name = year.getYearName() != null ? year.getYearName().trim() : "";
        return name.matches("\\d{4}") ? LocalDate.of(Integer.parseInt(name), 1, 1) : null;
    }

    /** Last day of the year: a year after the start, minus a day (null when the start is unknown). */
    public static LocalDate yearEnd(AcademicYear year) {
        LocalDate start = yearStart(year);
        return start != null ? start.plusYears(1).minusDays(1) : null;
    }

    @Transactional(readOnly = true)
    public List<DayOfWeek> weekends() {
        String raw = systemSettingsService.getSettings().getWeekendDays();
        if (raw == null) return DEFAULT_WEEKENDS;
        List<DayOfWeek> days = new ArrayList<>();
        for (String s : raw.split(",")) {
            if (s.isBlank()) continue;
            try {
                days.add(DayOfWeek.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // an unknown name is skipped
            }
        }
        return days.stream().distinct().sorted().toList();
    }

    /** Saves the weekly off days (an empty list = no weekend). At most six days, so a week keeps a class day. */
    @Transactional
    public List<DayOfWeek> setWeekends(List<String> names) {
        Set<DayOfWeek> days = new TreeSet<>();
        for (String n : names != null ? names : List.<String>of()) {
            try {
                days.add(DayOfWeek.valueOf(n.trim().toUpperCase()));
            } catch (RuntimeException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown day: " + n);
            }
        }
        if (days.size() > 6) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one day a week must be a school day");
        SystemSettings settings = systemSettingsService.getSettings();
        settings.setWeekendDays(days.stream().map(DayOfWeek::name).collect(Collectors.joining(",")));
        systemSettingsRepository.save(settings);
        return List.copyOf(days);
    }

    /**
     * Class days from {@code from} (inclusive) to {@code to} (exclusive): days that are not a weekend and not inside
     * an active Holiday entry of the given year. Null when either date is missing.
     */
    @Transactional(readOnly = true)
    public Integer classDays(Integer academicYearId, LocalDate from, LocalDate to) {
        if (from == null || to == null) return null;
        if (!from.isBefore(to)) return 0;
        Set<DayOfWeek> weekends = new HashSet<>(weekends());
        List<AcademicCalendarEntry> holidays = academicYearId == null ? List.of()
                : calendarRepository.findByAcademicYear_IdAndTypeAndIsActiveTrueOrderByStartDateAsc(academicYearId, CalendarEntryType.HOLIDAY);
        int count = 0;
        for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
            if (weekends.contains(d.getDayOfWeek())) continue;
            final LocalDate day = d;
            if (holidays.stream().anyMatch(h -> !day.isBefore(h.getStartDate()) && !day.isAfter(h.getEndDate()))) continue;
            count++;
        }
        return count;
    }
}
