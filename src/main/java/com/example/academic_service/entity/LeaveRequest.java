package com.example.academic_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_request",
        indexes = {
                @Index(name = "idx_leave_request_staff", columnList = "staff_id"),
                @Index(name = "idx_leave_request_status", columnList = "status")
        })
@Getter
@Setter
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @JsonProperty("staffId")
    public Long getStaffId() { return staff != null ? staff.getId() : null; }

    @JsonProperty("staffName")
    public String getStaffName() { return staff != null ? staff.getNameEnglish() : null; }

    @JsonProperty("staffSystemId")
    public String getStaffSystemId() { return staff != null ? staff.getStaffSystemId() : null; }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "current_tier_order")
    private Integer currentTierOrder = 1;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
