package com.example.academic_service.service;

import com.example.academic_service.entity.Enrollment;
import com.example.academic_service.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CacheWarmingService {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final ResultService resultService;
    private final CacheManager cacheManager;

    @Async
    public void warmStudentResultCache(Integer routineId, Integer academicYearId) {
        List<Enrollment> enrollments = enrollmentRepository.findByAcademicYearIdAndIsActiveTrue(academicYearId);
        int warmed = 0;
        for (Enrollment e : enrollments) {
            try {
                resultService.getStudentRoutineResult(e.getId(), routineId);
                warmed++;
            } catch (Exception ex) {
                log.debug("Cache warm skip enrollmentId={} routineId={}: {}", e.getId(), routineId, ex.getMessage());
            }
        }
        log.info("Cache warmed: {} / {} enrollments for routineId={}", warmed, enrollments.size(), routineId);
    }

    @Async
    public void evictStudentResultCache(Integer routineId, Integer academicYearId) {
        Cache cache = cacheManager.getCache("studentResult");
        if (cache == null) return;
        List<Enrollment> enrollments = enrollmentRepository.findByAcademicYearIdAndIsActiveTrue(academicYearId);
        enrollments.forEach(e -> cache.evict(e.getId() + ":" + routineId));
        log.info("Cache evicted {} entries for routineId={}", enrollments.size(), routineId);
    }
}
