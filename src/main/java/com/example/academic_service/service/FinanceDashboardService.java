package com.example.academic_service.service;

import com.example.academic_service.dto.dashboard.*;
import com.example.academic_service.entity.*;
import com.example.academic_service.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Aggregations for the school's finance dashboard.
 *
 * All numbers exclude CANCELLED invoices — those don't count toward
 * receivables. Payments table only holds INITIATED + SUCCESS (failures are
 * archived elsewhere), so payment queries don't need an extra status filter.
 */
@Service
@RequiredArgsConstructor
public class FinanceDashboardService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    private final EnrollmentRepository enrollmentRepo;

    @PersistenceContext
    private EntityManager em;

    // ── Summary ───────────────────────────────────────────────────────────

    public FinanceSummaryDto getSummary(LocalDate period) {
        LocalDate p = (period != null ? period : LocalDate.now()).withDayOfMonth(1);

        long activeEnrollments = countActiveEnrollments();

        // Single grouped query — counts + amounts per status for the period.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("""
            SELECT i.status,
                   COUNT(i),
                   COALESCE(SUM(i.totalAmount), 0),
                   COALESCE(SUM(i.paidAmount), 0)
              FROM Invoice i
             WHERE i.billingPeriod = :period
               AND i.status <> com.example.academic_service.entity.InvoiceStatus.CANCELLED
             GROUP BY i.status
        """).setParameter("period", p).getResultList();

        long fullyPaidCount = 0, partialCount = 0, pendingCount = 0, overdueCount = 0;
        BigDecimal fullyPaidAmt = BigDecimal.ZERO, partialAmt = BigDecimal.ZERO,
                pendingAmt = BigDecimal.ZERO, overdueAmt = BigDecimal.ZERO;
        BigDecimal totalInvoiced = BigDecimal.ZERO, totalCollected = BigDecimal.ZERO;

        for (Object[] r : rows) {
            InvoiceStatus s = (InvoiceStatus) r[0];
            long cnt = ((Number) r[1]).longValue();
            BigDecimal amt = (BigDecimal) r[2];
            BigDecimal paid = (BigDecimal) r[3];
            totalInvoiced = totalInvoiced.add(amt);
            totalCollected = totalCollected.add(paid);
            switch (s) {
                case PAID    -> { fullyPaidCount = cnt; fullyPaidAmt = amt; }
                case PARTIAL -> { partialCount = cnt; partialAmt = amt.subtract(paid); }
                case PENDING -> { pendingCount = cnt; pendingAmt = amt; }
                case OVERDUE -> { overdueCount = cnt; overdueAmt = amt; }
                default -> { /* CANCELLED already excluded */ }
            }
        }

        BigDecimal outstanding = totalInvoiced.subtract(totalCollected);
        BigDecimal rate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.multiply(BigDecimal.valueOf(100))
                        .divide(totalInvoiced, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new FinanceSummaryDto(p, activeEnrollments,
                totalInvoiced, totalCollected, outstanding, rate,
                fullyPaidCount, partialCount, pendingCount, overdueCount,
                fullyPaidAmt, partialAmt, pendingAmt, overdueAmt);
    }

    // ── Payment Status Breakdown ──────────────────────────────────────────

    public PaymentStatusBreakdownDto getPaymentStatusBreakdown(LocalDate period) {
        LocalDate p = (period != null ? period : LocalDate.now()).withDayOfMonth(1);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("""
            SELECT i.status, COUNT(i), COALESCE(SUM(i.totalAmount), 0)
              FROM Invoice i
             WHERE i.billingPeriod = :period
               AND i.status <> com.example.academic_service.entity.InvoiceStatus.CANCELLED
             GROUP BY i.status
        """).setParameter("period", p).getResultList();

        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<PaymentStatusBreakdownDto.Segment> segments = new ArrayList<>();
        for (Object[] r : rows) {
            InvoiceStatus s = (InvoiceStatus) r[0];
            long cnt = ((Number) r[1]).longValue();
            BigDecimal amt = (BigDecimal) r[2];
            BigDecimal pct = total > 0
                    ? BigDecimal.valueOf(cnt).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            segments.add(new PaymentStatusBreakdownDto.Segment(s, cnt, amt, pct));
        }
        // Stable display order — paid → partial → pending → overdue.
        segments.sort(Comparator.comparingInt(seg -> displayOrder(seg.getStatus())));
        return new PaymentStatusBreakdownDto(p, segments);
    }

    private int displayOrder(InvoiceStatus s) {
        return switch (s) {
            case PAID -> 0;
            case PARTIAL -> 1;
            case PENDING -> 2;
            case OVERDUE -> 3;
            case CANCELLED -> 4;
        };
    }

    // ── Collection Trend ──────────────────────────────────────────────────

    public CollectionTrendDto getCollectionTrend(int months) {
        if (months <= 0) months = 6;
        if (months > 24) months = 24;
        LocalDate end = LocalDate.now().withDayOfMonth(1);
        LocalDate start = end.minusMonths(months - 1L);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("""
            SELECT i.billingPeriod,
                   COALESCE(SUM(i.totalAmount), 0),
                   COALESCE(SUM(i.paidAmount), 0)
              FROM Invoice i
             WHERE i.billingPeriod BETWEEN :from AND :to
               AND i.status <> com.example.academic_service.entity.InvoiceStatus.CANCELLED
             GROUP BY i.billingPeriod
             ORDER BY i.billingPeriod
        """).setParameter("from", start).setParameter("to", end).getResultList();

        Map<LocalDate, BigDecimal[]> byMonth = new HashMap<>();
        for (Object[] r : rows) {
            byMonth.put((LocalDate) r[0],
                    new BigDecimal[]{(BigDecimal) r[1], (BigDecimal) r[2]});
        }

        // Build the full series even for months with no invoices.
        List<CollectionTrendDto.MonthPoint> series = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            BigDecimal[] amts = byMonth.getOrDefault(cursor,
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal invoiced = amts[0];
            BigDecimal collected = amts[1];
            series.add(new CollectionTrendDto.MonthPoint(
                    cursor, cursor.format(MONTH_LABEL),
                    invoiced, collected, invoiced.subtract(collected)));
            cursor = cursor.plusMonths(1);
        }
        return new CollectionTrendDto(series);
    }

    // ── Top Defaulters ────────────────────────────────────────────────────

    public TopDefaultersDto getTopDefaulters(int limit) {
        if (limit <= 0) limit = 10;
        if (limit > 100) limit = 100;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("""
            SELECT i.enrollmentId,
                   COALESCE(SUM(i.totalAmount - i.paidAmount), 0) AS outstanding,
                   COUNT(i),
                   MIN(i.billingPeriod)
              FROM Invoice i
             WHERE i.status IN (
                       com.example.academic_service.entity.InvoiceStatus.PENDING,
                       com.example.academic_service.entity.InvoiceStatus.PARTIAL,
                       com.example.academic_service.entity.InvoiceStatus.OVERDUE)
             GROUP BY i.enrollmentId
             HAVING SUM(i.totalAmount - i.paidAmount) > 0
             ORDER BY outstanding DESC
        """).setMaxResults(limit).getResultList();

        List<TopDefaultersDto.Defaulter> out = new ArrayList<>();
        for (Object[] r : rows) {
            Long enrollmentId = ((Number) r[0]).longValue();
            BigDecimal outstanding = (BigDecimal) r[1];
            int count = ((Number) r[2]).intValue();
            LocalDate oldest = (LocalDate) r[3];

            Enrollment e = enrollmentRepo.findById(enrollmentId).orElse(null);
            String studentName = null, className = null, systemId = null;
            if (e != null) {
                if (e.getStudent() != null) {
                    studentName = e.getStudent().getNameEnglish();
                    systemId = e.getStudent().getStudentSystemId();
                }
                if (e.getStudentClass() != null) className = e.getStudentClass().getName();
            }
            out.add(new TopDefaultersDto.Defaulter(
                    enrollmentId, systemId, studentName, className,
                    outstanding, count, oldest));
        }
        return new TopDefaultersDto(out);
    }

    // ── Recent Payments ───────────────────────────────────────────────────

    public RecentPaymentsDto getRecentPayments(int limit) {
        if (limit <= 0) limit = 20;
        if (limit > 100) limit = 100;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("""
            SELECT p.id, p.tranId, p.invoiceId, p.amount, p.method, p.completedAt
              FROM Payment p
             WHERE p.status = com.example.academic_service.entity.PaymentStatus.SUCCESS
             ORDER BY p.completedAt DESC, p.id DESC
        """).setMaxResults(limit).getResultList();

        // Bulk load invoices and enrollments for names.
        Set<Long> invoiceIds = new HashSet<>();
        for (Object[] r : rows) invoiceIds.add(((Number) r[2]).longValue());
        Map<Long, Invoice> invoiceById = new HashMap<>();
        if (!invoiceIds.isEmpty()) {
            for (Invoice inv : em.createQuery(
                    "SELECT i FROM Invoice i WHERE i.id IN :ids", Invoice.class)
                    .setParameter("ids", invoiceIds).getResultList()) {
                invoiceById.put(inv.getId(), inv);
            }
        }
        Set<Long> enrollmentIds = new HashSet<>();
        for (Invoice inv : invoiceById.values()) enrollmentIds.add(inv.getEnrollmentId());
        Map<Long, Enrollment> enrollmentById = new HashMap<>();
        if (!enrollmentIds.isEmpty()) {
            for (Enrollment e : enrollmentRepo.findAllById(enrollmentIds)) {
                enrollmentById.put(e.getId(), e);
            }
        }

        List<RecentPaymentsDto.RecentPayment> list = new ArrayList<>();
        for (Object[] r : rows) {
            Long paymentId = ((Number) r[0]).longValue();
            String tranId = (String) r[1];
            Long invoiceId = ((Number) r[2]).longValue();
            BigDecimal amount = (BigDecimal) r[3];
            PaymentMethod method = (PaymentMethod) r[4];
            LocalDateTime completedAt = (LocalDateTime) r[5];

            Invoice inv = invoiceById.get(invoiceId);
            String invoiceNumber = inv != null ? inv.getInvoiceNumber() : null;
            String studentName = null;
            if (inv != null) {
                Enrollment e = enrollmentById.get(inv.getEnrollmentId());
                if (e != null && e.getStudent() != null) {
                    studentName = e.getStudent().getNameEnglish();
                }
            }
            list.add(new RecentPaymentsDto.RecentPayment(
                    paymentId, tranId, studentName, invoiceNumber,
                    amount, method, completedAt));
        }
        return new RecentPaymentsDto(list);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private long countActiveEnrollments() {
        Object r = em.createNativeQuery(
                "SELECT COUNT(*) FROM enrollment WHERE is_active = TRUE").getSingleResult();
        return ((Number) r).longValue();
    }
}
