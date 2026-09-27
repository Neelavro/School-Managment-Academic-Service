package com.example.academic_service.repository;

/** Just the columns student login needs — skips the eager gender/status/image joins. */
public interface StudentLoginView {
    Long getId();
    String getStudentSystemId();
    String getNameEnglish();
    Boolean getIsActive();
    String getPasswordHash();
}
