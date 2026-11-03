package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "heading")
    private String heading;

    @Column(name = "signature_url")
    private String signatureUrl;

    /** The school's weekly off days, comma-separated java.time.DayOfWeek names ("FRIDAY,SATURDAY"); null = Friday. */
    @Column(name = "weekend_days", length = 64)
    private String weekendDays;
}
