package com.example.academic_service.service;

import com.example.academic_service.exception.ServerBusyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Admission control for student password hashing (BCrypt is deliberately CPU-heavy).
 *
 * Without it, a result-day burst puts every Tomcat thread on BCrypt at once: each
 * login slows to the pace of the whole crowd, the rest of the API starves, and
 * excess connections get reset. With it, at most {@code maxConcurrent} hashes run,
 * at most {@code maxQueue} requests wait (a blocked thread costs no CPU), and
 * everyone beyond that gets an immediate 429 so the app can retry shortly.
 */
@Component
public class StudentLoginLimiter {

    private final Semaphore permits;
    private final AtomicInteger waiting = new AtomicInteger();
    private final int maxQueue;
    private final long maxWaitMs;

    public StudentLoginLimiter(
            @Value("${auth.student-login.max-concurrent:0}") int maxConcurrent,
            @Value("${auth.student-login.max-queue:50}") int maxQueue,
            @Value("${auth.student-login.max-wait-ms:5000}") long maxWaitMs) {
        int n = maxConcurrent > 0 ? maxConcurrent : Runtime.getRuntime().availableProcessors();
        this.permits = new Semaphore(n, true);
        this.maxQueue = maxQueue;
        this.maxWaitMs = maxWaitMs;
    }

    public <T> T run(Supplier<T> hashingWork) {
        if (waiting.incrementAndGet() > maxQueue) {
            waiting.decrementAndGet();
            throw busy();
        }
        boolean acquired;
        try {
            acquired = permits.tryAcquire(maxWaitMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            acquired = false;
        } finally {
            waiting.decrementAndGet();
        }
        if (!acquired) throw busy();
        try {
            return hashingWork.get();
        } finally {
            permits.release();
        }
    }

    private ServerBusyException busy() {
        return new ServerBusyException("Too many students are signing in right now. Please try again in a moment.");
    }
}
