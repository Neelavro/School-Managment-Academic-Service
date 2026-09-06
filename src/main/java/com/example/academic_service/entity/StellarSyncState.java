package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stellar_sync_state")
@Getter
@Setter
public class StellarSyncState {

    // Single-row table; id is always 1.
    @Id
    private Integer id;

    @Column(name = "last_access_id", nullable = false)
    private Long lastAccessId = 0L;

    @Column(name = "last_call_at")
    private LocalDateTime lastCallAt;

    @Column(name = "last_today_count")
    private Integer lastTodayCount;

    @Column(name = "last_today_count_date")
    private java.time.LocalDate lastTodayCountDate;
}
