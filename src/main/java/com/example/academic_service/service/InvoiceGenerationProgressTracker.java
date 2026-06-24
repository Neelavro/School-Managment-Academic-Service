package com.example.academic_service.service;

import com.example.academic_service.dto.InvoiceGenerationProgress;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory progress map for async invoice generation. The frontend polls
 * GET /api/accounting/invoices/generate/progress/{taskId} every ~500ms and
 * reads {processed, total} to render a real progress bar.
 *
 * No persistence — a task that's still RUNNING when the JVM restarts is lost,
 * which is fine because the frontend would have abandoned polling anyway.
 *
 * Tasks are evicted on read after DONE/FAILED so the map self-cleans.
 */
@Service
public class InvoiceGenerationProgressTracker {

    private final ConcurrentHashMap<String, InvoiceGenerationProgress> tasks = new ConcurrentHashMap<>();

    public InvoiceGenerationProgress start(int total) {
        InvoiceGenerationProgress p = new InvoiceGenerationProgress();
        p.setTaskId(UUID.randomUUID().toString());
        p.setStatus(InvoiceGenerationProgress.Status.RUNNING);
        p.setProcessed(0);
        p.setTotal(total);
        tasks.put(p.getTaskId(), p);
        return p;
    }

    public void increment(String taskId) {
        InvoiceGenerationProgress p = tasks.get(taskId);
        if (p != null) p.setProcessed(p.getProcessed() + 1);
    }

    public void setTotal(String taskId, int total) {
        InvoiceGenerationProgress p = tasks.get(taskId);
        if (p != null) p.setTotal(total);
    }

    public void complete(String taskId, com.example.academic_service.dto.InvoiceGenerationResult result) {
        InvoiceGenerationProgress p = tasks.get(taskId);
        if (p == null) return;
        p.setStatus(InvoiceGenerationProgress.Status.DONE);
        p.setResult(result);
        p.setProcessed(p.getTotal());
    }

    public void fail(String taskId, String error) {
        InvoiceGenerationProgress p = tasks.get(taskId);
        if (p == null) return;
        p.setStatus(InvoiceGenerationProgress.Status.FAILED);
        p.setError(error);
    }

    /**
     * Returns the current snapshot. When the task is terminal (DONE / FAILED),
     * the entry is evicted after this call so memory doesn't grow forever.
     */
    public InvoiceGenerationProgress get(String taskId) {
        InvoiceGenerationProgress p = tasks.get(taskId);
        if (p != null && p.getStatus() != InvoiceGenerationProgress.Status.RUNNING) {
            tasks.remove(taskId);
        }
        return p;
    }
}
