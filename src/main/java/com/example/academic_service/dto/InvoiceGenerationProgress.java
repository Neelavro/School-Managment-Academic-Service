package com.example.academic_service.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Snapshot of an in-flight invoice generation. Returned by the progress
 * polling endpoint so the UI can drive a 0–100% progress bar based on
 * processed / total.
 *
 *   status RUNNING  → task is still iterating enrollments
 *   status DONE     → finished successfully, result is populated
 *   status FAILED   → generation aborted with an error before any rows ran
 */
@Getter
@Setter
public class InvoiceGenerationProgress {
    public enum Status { RUNNING, DONE, FAILED }

    private String taskId;
    private Status status;
    private int processed;
    private int total;
    private InvoiceGenerationResult result;   // populated on DONE
    private String error;                     // populated on FAILED
}
