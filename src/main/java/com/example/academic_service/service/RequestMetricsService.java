package com.example.academic_service.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-memory traffic counter. Tracks the last 24 hours as 1440 per-minute buckets.
 * Lost on restart — that's fine for a rolling window.
 *
 * Total RAM footprint: ~50 KB. Lock-free reads via AtomicLong / LongAdder.
 *
 * Bucketing: each minute since boot has an index = (epochMinute % 1440).
 * Every record() resets stale buckets (older than 24h) before incrementing.
 */
@Service
public class RequestMetricsService {

    private static final int BUCKETS = 24 * 60;

    /** Total requests per minute bucket. */
    private final LongAdder[] hits = new LongAdder[BUCKETS];
    /** Sum of response times (ms) per bucket — for computing average. */
    private final LongAdder[] timeSumMs = new LongAdder[BUCKETS];
    /** 4xx counter per bucket. */
    private final LongAdder[] err4xx = new LongAdder[BUCKETS];
    /** 5xx counter per bucket. */
    private final LongAdder[] err5xx = new LongAdder[BUCKETS];
    /** Last minute we wrote into this bucket — for staleness detection. */
    private final AtomicLong[] bucketMinute = new AtomicLong[BUCKETS];

    @Getter
    private final long startedAtEpochMs = System.currentTimeMillis();

    public RequestMetricsService() {
        for (int i = 0; i < BUCKETS; i++) {
            hits[i] = new LongAdder();
            timeSumMs[i] = new LongAdder();
            err4xx[i] = new LongAdder();
            err5xx[i] = new LongAdder();
            bucketMinute[i] = new AtomicLong(0);
        }
    }

    public void record(int httpStatus, long durationMs) {
        long minute = System.currentTimeMillis() / 60_000L;
        int idx = (int) Math.floorMod(minute, BUCKETS);

        // If this bucket holds older data, reset it before we write.
        long stored = bucketMinute[idx].get();
        if (stored != minute) {
            if (bucketMinute[idx].compareAndSet(stored, minute)) {
                hits[idx].reset();
                timeSumMs[idx].reset();
                err4xx[idx].reset();
                err5xx[idx].reset();
            }
        }
        hits[idx].increment();
        timeSumMs[idx].add(durationMs);
        if (httpStatus >= 400 && httpStatus < 500) err4xx[idx].increment();
        if (httpStatus >= 500) err5xx[idx].increment();
    }

    public Stats snapshot() {
        long nowMin = System.currentTimeMillis() / 60_000L;
        long fromMin = nowMin - BUCKETS + 1;

        long total = 0, totalErr4 = 0, totalErr5 = 0, totalMs = 0;
        long peakRpm = 0;
        long[] perHour = new long[24];
        for (int i = 0; i < BUCKETS; i++) {
            long bMin = bucketMinute[i].get();
            if (bMin < fromMin || bMin == 0) continue; // skip stale / never used
            long h = hits[i].sum();
            total += h;
            totalErr4 += err4xx[i].sum();
            totalErr5 += err5xx[i].sum();
            totalMs += timeSumMs[i].sum();
            if (h > peakRpm) peakRpm = h;
            int hour = (int) Math.floorMod(bMin / 60, 24);
            perHour[hour] += h;
        }
        int peakH = 0;
        long peakHCount = 0;
        for (int h = 0; h < 24; h++) {
            if (perHour[h] > peakHCount) { peakHCount = perHour[h]; peakH = h; }
        }
        return new Stats(
                total,
                total > 0 ? (double) totalErr4 / total : 0,
                total > 0 ? (double) totalErr5 / total : 0,
                String.format("%02d:00", peakH),
                peakRpm,
                total > 0 ? (double) totalMs / total : 0
        );
    }

    public LocalDateTime startedAt() {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startedAtEpochMs),
                ZoneId.systemDefault());
    }

    public record Stats(
            long requestsLast24h,
            double errorRate4xx,
            double errorRate5xx,
            String peakHour,
            long peakRequestsPerMinute,
            double avgResponseTimeMs
    ) {}
}
