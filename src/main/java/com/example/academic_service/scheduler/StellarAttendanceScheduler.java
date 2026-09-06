package com.example.academic_service.scheduler;

import com.example.academic_service.service.StellarAttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "stellar.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StellarAttendanceScheduler {

    private final StellarAttendanceService service;

    // At the top of every hour from 07:00 through 14:00 inclusive, Asia/Dhaka.
    @Scheduled(cron = "0 0 7-14 * * *", zone = "Asia/Dhaka")
    public void pullAttendance() {
        try {
            StellarAttendanceService.IngestResult r = service.ingestToday();
            log.info("Stellar cron: called={} inserted={} maxAccessId={}",
                    r.called(), r.inserted(), r.lastAccessId());
        } catch (Exception e) {
            log.error("Stellar cron failed", e);
        }
    }
}
