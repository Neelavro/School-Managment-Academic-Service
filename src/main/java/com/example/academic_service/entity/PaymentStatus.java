package com.example.academic_service.entity;

public enum PaymentStatus {
    INITIATED,   // session created, parent redirected to gateway
    SUCCESS,     // validated by gateway, journal entries posted
    FAILED,      // explicitly failed at gateway or amount mismatch
    CANCELLED    // parent cancelled at gateway
}
