package com.example.academic_service.service.impl;

import com.example.academic_service.dto.ApiResponse;
import com.example.academic_service.dto.DashboardSummaryResponseDto;
import com.example.academic_service.dto.DashboardSummaryResponseDto.ClassBreakdown;
import com.example.academic_service.dto.DashboardSummaryResponseDto.GenderBucket;
import com.example.academic_service.entity.AcademicYear;
import com.example.academic_service.repository.AcademicYearRepository;
import com.example.academic_service.repository.EnrollmentRepository;
import com.example.academic_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AcademicYearRepository academicYearRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<DashboardSummaryResponseDto> getSummary() {
        AcademicYear year = academicYearRepository.findFirstByIsActiveTrue().orElse(null);
        if (year == null) {
            // No active year yet — return an empty but well-shaped payload so
            // the frontend can render "no data" instead of erroring out.
            return ApiResponse.success("Dashboard summary fetched",
                    new DashboardSummaryResponseDto(Instant.now(), null, 0L,
                            Collections.emptyList(), Collections.emptyList()));
        }

        List<Object[]> rows = enrollmentRepository.countByClassAndGenderSection(year.getId());

        // Aggregate rows into per-class buckets while also tallying the
        // overall totals, so we walk the result set exactly once.
        Map<Integer, ClassAgg> classAggById = new LinkedHashMap<>();
        Map<Integer, GenderBucket> overallByGenderId = new LinkedHashMap<>();
        long total = 0;

        for (Object[] row : rows) {
            Integer classId    = toInt(row[0]);
            String  className  = row[1] != null ? row[1].toString() : "";
            Integer genderId   = toInt(row[2]);
            String  genderName = row[3] != null ? row[3].toString() : "";
            long    count      = row[4] instanceof Number cn ? cn.longValue() : 0L;
            total += count;

            ClassAgg agg = classAggById.computeIfAbsent(classId,
                    k -> new ClassAgg(classId, className));
            agg.total += count;
            agg.buckets.add(new GenderBucket(genderId, genderName, count));

            overallByGenderId.merge(genderId,
                    new GenderBucket(genderId, genderName, count),
                    (existing, incoming) -> {
                        existing.setCount(existing.getCount() + incoming.getCount());
                        return existing;
                    });
        }

        List<ClassBreakdown> byClass = new ArrayList<>();
        for (ClassAgg agg : classAggById.values()) {
            byClass.add(new ClassBreakdown(agg.classId, agg.className, agg.total, agg.buckets));
        }

        DashboardSummaryResponseDto dto = new DashboardSummaryResponseDto(
                Instant.now(),
                year.getYearName(),
                total,
                new ArrayList<>(overallByGenderId.values()),
                byClass);
        return ApiResponse.success("Dashboard summary fetched", dto);
    }

    private static Integer toInt(Object v) {
        return v instanceof Number n ? n.intValue() : null;
    }

    private static class ClassAgg {
        final Integer classId;
        final String className;
        long total = 0;
        final List<GenderBucket> buckets = new ArrayList<>();
        ClassAgg(Integer classId, String className) {
            this.classId = classId;
            this.className = className;
        }
    }
}
