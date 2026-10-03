package com.example.academic_service.entity;

public enum PayrollRunStatus {
    DRAFT,      // Payslips can be changed; nothing posted
    FINALISED,  // Expense posted, salaries owed; payslips are paid from here
    CANCELLED   // Finalised by mistake and reversed before anyone was paid
}
