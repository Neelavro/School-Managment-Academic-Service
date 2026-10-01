package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A notice for the school website's notice board, written in the admin panel. */
@Entity
@Table(name = "notice")
@Getter
@Setter
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title_bn")
    private String titleBn;

    @Column(name = "title_en")
    private String titleEn;

    @Column(name = "body_bn", columnDefinition = "TEXT")
    private String bodyBn;

    @Column(name = "body_en", columnDefinition = "TEXT")
    private String bodyEn;

    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;

    /** general | exam | result | admission | holiday | tender (the website's categories). */
    @Column(nullable = false)
    private String category = "general";

    @Column(nullable = false)
    private Boolean pinned = false;

    @Column(nullable = false)
    private Boolean published = true;

    /** Stored file name in the images folder (never sent to clients). */
    @Column(name = "attachment_file")
    private String attachmentFile;

    /** The uploader's file name, used for downloads. */
    @Column(name = "attachment_name")
    private String attachmentName;

    @Column(name = "attachment_type")
    private String attachmentType;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(AdmissionApplication.DHAKA);

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
