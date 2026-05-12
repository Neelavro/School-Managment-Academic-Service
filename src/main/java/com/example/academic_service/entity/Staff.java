package com.example.academic_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "staff",
        indexes = {
                @Index(name = "idx_staff_system_id",     columnList = "staff_system_id"),
                @Index(name = "idx_staff_employee_type", columnList = "employee_type"),
                @Index(name = "idx_staff_active",        columnList = "is_active")
        })
@Getter
@Setter
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_system_id", unique = true)
    private String staffSystemId;

    // ── Identity ──────────────────────────────────────────────────────────────
    @Column(name = "name_english", nullable = false)
    private String nameEnglish;

    @Column(name = "name_bangla")
    private String nameBangla;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "spouse_name")
    private String spouseName;

    @Column(name = "dob")
    private LocalDate dob;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_id")
    private Gender gender;

    @Column(name = "religion")
    private String religion;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "nationality")
    private String nationality;

    // ── ID Documents ──────────────────────────────────────────────────────────
    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "smart_card_number")
    private String smartCardNumber;

    @Column(name = "birth_reg_number")
    private String birthRegNumber;

    // ── Contact ───────────────────────────────────────────────────────────────
    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    // Present address — Bangladesh structure
    @Column(name = "present_division")
    private String presentDivision;

    @Column(name = "present_district")
    private String presentDistrict;

    @Column(name = "present_upazila")
    private String presentUpazila;

    @Column(name = "present_union")
    private String presentUnion;

    @Column(name = "present_post_office")
    private String presentPostOffice;

    @Column(name = "present_village")
    private String presentVillage;

    // Permanent address
    @Column(name = "permanent_division")
    private String permanentDivision;

    @Column(name = "permanent_district")
    private String permanentDistrict;

    @Column(name = "permanent_upazila")
    private String permanentUpazila;

    @Column(name = "permanent_union")
    private String permanentUnion;

    @Column(name = "permanent_post_office")
    private String permanentPostOffice;

    @Column(name = "permanent_village")
    private String permanentVillage;

    // ── Employment ────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false)
    private EmployeeType employeeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Column(name = "department")
    private String department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_designation_id")
    private Designation currentDesignation;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    // ── Financial ─────────────────────────────────────────────────────────────
    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_routing_number")
    private String bankRoutingNumber;

    @Column(name = "bkash_number")
    private String bkashNumber;

    @Column(name = "e_tin")
    private String eTin;

    // ── Status ────────────────────────────────────────────────────────────────
    @Column(name = "is_active")
    private Boolean isActive = true;
}
