package com.example.academic_service.scheduler;

import com.example.academic_service.service.LateFeeService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Adds late fees to overdue monthly fees: a minute after start-up, then every hour.
 * Its own executor on purpose — @EnableScheduling is off in this app, and turning it
 * on would also start the dormant Stellar attendance cron.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LateFeeScheduler {

    private final LateFeeService lateFeeService;
    private ScheduledExecutorService executor;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "late-fee");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(this::run, 1, 60, TimeUnit.MINUTES);
    }

    private void run() {
        try {
            int added = lateFeeService.applyAllOverdue();
            if (added > 0) log.info("Late fee added to {} monthly fee(s)", added);
        } catch (Exception e) {
            log.error("Late fee run failed", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (executor != null) executor.shutdownNow();
    }
}
