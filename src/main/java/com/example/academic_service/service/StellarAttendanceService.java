package com.example.academic_service.service;

import com.example.academic_service.config.StellarProperties;
import com.example.academic_service.entity.StellarAttendanceLog;
import com.example.academic_service.entity.StellarSyncState;
import com.example.academic_service.repository.StellarAttendanceLogRepository;
import com.example.academic_service.repository.StellarSyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "stellar.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StellarAttendanceService {

    private static final int SYNC_STATE_ID = 1;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StellarApiClient client;
    private final StellarAttendanceLogRepository logRepo;
    private final StellarSyncStateRepository stateRepo;
    private final StellarProperties props;

    // Called by the cron. Persists any new punches and updates the cursor.
    @Transactional
    public IngestResult ingestToday() {
        StellarSyncState state = loadState();
        if (!allowCall(state, props.getMinCallIntervalSeconds())) {
            log.info("Stellar cron skipped — last call at {} is within {}s floor",
                    state.getLastCallAt(), props.getMinCallIntervalSeconds());
            return new IngestResult(0, state.getLastAccessId(), false);
        }

        LocalDate today = LocalDate.now(ZoneId.of(props.getTimezone()));
        List<StellarApiClient.LogEntry> entries = client.fetchLog(
                today, today, LocalTime.MIN, LocalTime.of(23, 59, 59),
                state.getLastAccessId());

        state.setLastCallAt(LocalDateTime.now());

        if (entries.isEmpty()) {
            stateRepo.save(state);
            return new IngestResult(0, state.getLastAccessId(), true);
        }

        int inserted = 0;
        long maxAccessId = state.getLastAccessId();
        for (StellarApiClient.LogEntry e : entries) {
            long accessId = parseLong(e.access_id);
            if (accessId > maxAccessId) maxAccessId = accessId;
            try {
                logRepo.save(toEntity(e, accessId));
                inserted++;
            } catch (DataIntegrityViolationException dup) {
                // duplicate access_id — safe to ignore
            }
        }

        state.setLastAccessId(maxAccessId);

        // Refresh dashboard cache from DB so it reflects the just-ingested batch.
        long uniqueToday = logRepo.countDistinctRegistrationIdsByDate(today);
        state.setLastTodayCount((int) uniqueToday);
        state.setLastTodayCountDate(today);

        stateRepo.save(state);
        return new IngestResult(inserted, maxAccessId, true);
    }

    // Called by the dashboard endpoint. Returns unique-student count for today
    // without persisting anything. Reuses the cached count if within the
    // dashboard cache window; otherwise makes a read-only Stellar call.
    @Transactional
    public TodayCount getTodayCount() {
        LocalDate today = LocalDate.now(ZoneId.of(props.getTimezone()));
        StellarSyncState state = loadState();

        boolean cacheFresh = state.getLastCallAt() != null
                && today.equals(state.getLastTodayCountDate())
                && state.getLastCallAt().isAfter(
                        LocalDateTime.now().minusSeconds(props.getDashboardCacheSeconds()));
        if (cacheFresh) {
            return new TodayCount(nvl(state.getLastTodayCount()), state.getLastCallAt(), true);
        }

        if (!allowCall(state, props.getMinCallIntervalSeconds())) {
            // Respect Stellar's 5-min floor; fall back to whatever we have.
            return new TodayCount(nvl(state.getLastTodayCount()), state.getLastCallAt(), true);
        }

        // Read-only: start from accessId=0 for the today window so we get the full day's
        // unique student count, independent of the cron's rolling cursor.
        List<StellarApiClient.LogEntry> entries = client.fetchLog(
                today, today, LocalTime.MIN, LocalTime.of(23, 59, 59), 0L);

        int uniqueCount = (int) entries.stream()
                .map(e -> e.registration_id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();

        state.setLastCallAt(LocalDateTime.now());
        state.setLastTodayCount(uniqueCount);
        state.setLastTodayCountDate(today);
        stateRepo.save(state);

        return new TodayCount(uniqueCount, state.getLastCallAt(), false);
    }

    private StellarSyncState loadState() {
        return stateRepo.findById(SYNC_STATE_ID).orElseGet(() -> {
            StellarSyncState s = new StellarSyncState();
            s.setId(SYNC_STATE_ID);
            s.setLastAccessId(0L);
            return stateRepo.save(s);
        });
    }

    private boolean allowCall(StellarSyncState state, int floorSeconds) {
        if (state.getLastCallAt() == null) return true;
        return state.getLastCallAt().isBefore(LocalDateTime.now().minusSeconds(floorSeconds));
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }

    private int nvl(Integer v) { return v == null ? 0 : v; }

    private StellarAttendanceLog toEntity(StellarApiClient.LogEntry e, long accessId) {
        StellarAttendanceLog row = new StellarAttendanceLog();
        row.setAccessId(accessId);
        row.setRegistrationId(e.registration_id);
        row.setUserName(e.user_name);
        row.setUnitId(e.unit_id);
        row.setUnitName(e.unit_name);
        if (e.access_date != null) {
            try { row.setAccessDate(LocalDate.parse(e.access_date, DATE_FMT)); }
            catch (Exception ex) { /* leave null */ }
        }
        if (e.access_time != null) {
            try { row.setAccessTime(LocalTime.parse(e.access_time, TIME_FMT)); }
            catch (Exception ex) { /* leave null */ }
        }
        row.setCard(e.card);
        row.setReceivedAt(LocalDateTime.now());
        return row;
    }

    public record IngestResult(int inserted, long lastAccessId, boolean called) {}
    public record TodayCount(int count, LocalDateTime fetchedAt, boolean cached) {}
}
