package com.example.academic_service.service;

import com.example.academic_service.dto.platform.PlatformMetricsDto;
import com.example.academic_service.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gathers every metric the platform admin needs into one DTO.
 * Cached for 60 seconds via {@code platformMetrics} cache so repeated polls
 * don't hit the DB.
 */
@Service
@RequiredArgsConstructor
public class PlatformMetricsService {

    private final EnrollmentRepository enrollmentRepo;
    private final InvoiceRepository invoiceRepo;
    private final PaymentRepository paymentRepo;
    private final RequestMetricsService requestMetrics;
    private final FinanceDashboardService financeDashboard;

    @PersistenceContext
    private EntityManager em;

    @Cacheable(cacheNames = "platformMetrics", sync = true)
    public PlatformMetricsDto snapshot() {
        return new PlatformMetricsDto(
                LocalDateTime.now(),
                buildEnrollment(),
                buildActivity(),
                buildInfrastructure(),
                buildTraffic(),
                buildFinance());
    }

    private PlatformMetricsDto.Finance buildFinance() {
        var summary = financeDashboard.getSummary(null);
        return new PlatformMetricsDto.Finance(
                summary.getPeriod(),
                summary.getTotalInvoiced(),
                summary.getTotalCollected(),
                summary.getTotalOutstanding(),
                summary.getCollectionRate(),
                summary.getFullyPaidCount(),
                summary.getPartiallyPaidCount(),
                summary.getPendingCount(),
                summary.getOverdueCount());
    }

    // ── Sections ──────────────────────────────────────────────────────────

    private PlatformMetricsDto.Enrollment buildEnrollment() {
        long total = enrollmentRepo.count();
        long active = countActiveEnrollments();
        // "newThisMonth" and "droppedThisMonth" require a createdAt column on
        // Enrollment to compute. Keeping them at 0 for now — the platform
        // admin can compute deltas by diffing daily snapshots in
        // school_metric_snapshots.
        return new PlatformMetricsDto.Enrollment(active, total - active, 0L, 0L);
    }

    private PlatformMetricsDto.Activity buildActivity() {
        LocalDateTime monthStart = LocalDate.now()
                .withDayOfMonth(1).atStartOfDay();
        LocalDate dateMonthStart = monthStart.toLocalDate();

        long marks = countRowsSinceTimestamp("student_mark", "created_at", monthStart);
        long invoices = countRowsSinceDate("invoices", "issued_date", dateMonthStart);
        long payments = countRowsSinceTimestamp("payments", "initiated_at", monthStart);
        long attendance = countRowsSinceTimestamp("attendance", "created_at", monthStart);
        LocalDateTime lastInvoice = maxTimestamp("invoices", "created_at");
        LocalDateTime lastPayment = maxTimestamp("payments", "initiated_at");

        return new PlatformMetricsDto.Activity(
                marks, invoices, payments, attendance,
                lastInvoice, lastPayment);
    }

    private PlatformMetricsDto.Infrastructure buildInfrastructure() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long heapUsed = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);

        Long ramUsedMb = null, ramTotalMb = null;
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            long total = sunOs.getTotalMemorySize();
            long free = sunOs.getFreeMemorySize();
            ramTotalMb = total / (1024 * 1024);
            ramUsedMb = (total - free) / (1024 * 1024);
        }

        long dbBytes = scalar("""
            SELECT COALESCE(SUM(data_length + index_length), 0)
              FROM information_schema.tables
             WHERE table_schema = DATABASE()
        """);
        long tableCount = scalar("""
            SELECT COUNT(*)
              FROM information_schema.tables
             WHERE table_schema = DATABASE()
        """);

        return new PlatformMetricsDto.Infrastructure(
                runtime.getUptime() / 1000L,
                heapUsed, heapMax,
                ramUsedMb, ramTotalMb,
                dbBytes / (1024 * 1024),
                tableCount);
    }

    private PlatformMetricsDto.Traffic buildTraffic() {
        RequestMetricsService.Stats s = requestMetrics.snapshot();
        return new PlatformMetricsDto.Traffic(
                s.requestsLast24h(),
                round(s.errorRate4xx(), 4),
                round(s.errorRate5xx(), 4),
                s.peakHour(),
                s.peakRequestsPerMinute(),
                round(s.avgResponseTimeMs(), 1));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * EnrollmentRepository doesn't expose a count-by-isActive method, but the
     * underlying column is indexed. Use a quick native query.
     */
    private long countActiveEnrollments() {
        Object r = em.createNativeQuery(
                "SELECT COUNT(*) FROM enrollment WHERE is_active = TRUE").getSingleResult();
        return ((Number) r).longValue();
    }

    private long countRowsSinceTimestamp(String table, String column, LocalDateTime since) {
        try {
            Object r = em.createNativeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + column + " >= :since")
                    .setParameter("since", since)
                    .getSingleResult();
            return ((Number) r).longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private long countRowsSinceDate(String table, String column, LocalDate since) {
        try {
            Object r = em.createNativeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + column + " >= :since")
                    .setParameter("since", since)
                    .getSingleResult();
            return ((Number) r).longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private LocalDateTime maxTimestamp(String table, String column) {
        try {
            Object r = em.createNativeQuery(
                    "SELECT MAX(" + column + ") FROM " + table).getSingleResult();
            if (r == null) return null;
            if (r instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
            if (r instanceof LocalDateTime ldt) return ldt;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private long scalar(String sql) {
        try {
            Object r = em.createNativeQuery(sql).getSingleResult();
            return r != null ? ((Number) r).longValue() : 0L;
        } catch (Exception e) {
            return 0;
        }
    }

    private double round(double v, int dp) {
        double f = Math.pow(10, dp);
        return Math.round(v * f) / f;
    }
}
