package com.example.academic_service.dto.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full metric snapshot for the platform admin. One JSON blob, one HTTP call,
 * everything the platform owner needs to bill, monitor, and judge health.
 *
 * Cheap to compute (~150-300ms). Cached for 60s in MetricsCache.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformMetricsDto {
    private LocalDateTime snapshotAt;
    private Enrollment enrollment;
    private Activity activity;
    private Infrastructure infrastructure;
    private Traffic traffic;
    private Finance finance;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Enrollment {
        private long activeNow;
        private long inactiveNow;
        private long newThisMonth;
        private long droppedThisMonth;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Activity {
        private long marksEnteredThisMonth;
        private long invoicesGeneratedThisMonth;
        private long paymentsProcessedThisMonth;
        private long attendanceRecordsThisMonth;
        private LocalDateTime lastInvoiceGeneratedAt;
        private LocalDateTime lastPaymentAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Infrastructure {
        private long uptimeSeconds;
        private long jvmHeapUsedMb;
        private long jvmHeapMaxMb;
        /** Null on non-Linux JVMs (e.g. dev macOS without sun.management). */
        private Long systemRamUsedMb;
        private Long systemRamTotalMb;
        private long dbSizeMb;
        private long tableCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Traffic {
        private long requestsLast24h;
        private double errorRate4xxLast24h;
        private double errorRate5xxLast24h;
        /** "HH:00" hour-of-day with the most requests in the last 24h, e.g. "14:00". */
        private String peakHour;
        private long peakRequestsPerMinute;
        private double avgResponseTimeMs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Finance {
        /** Current billing month (first of month). */
        private LocalDate period;
        private BigDecimal totalInvoiced;
        private BigDecimal totalCollected;
        private BigDecimal totalOutstanding;
        /** 0-100 percentage. */
        private BigDecimal collectionRate;
        private long fullyPaidCount;
        private long partiallyPaidCount;
        private long pendingCount;
        private long overdueCount;
    }
}
