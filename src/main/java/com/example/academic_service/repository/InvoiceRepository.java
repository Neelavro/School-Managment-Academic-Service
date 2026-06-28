package com.example.academic_service.repository;

import com.example.academic_service.entity.Invoice;
import com.example.academic_service.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByEnrollmentIdAndBillingPeriod(Long enrollmentId, LocalDate billingPeriod);

    List<Invoice> findByEnrollmentIdAndBillingPeriodIn(Long enrollmentId, List<LocalDate> periods);

    /**
     * Used by invoice generation to skip enrollments that already have an ACTIVE
     * invoice for the given period. Cancelled invoices are intentionally NOT counted,
     * so regenerating after cancellation works.
     */
    @Query("""
        SELECT i.enrollmentId FROM Invoice i
        WHERE i.billingPeriod = :period
          AND i.enrollmentId IN :enrollmentIds
          AND i.status <> com.example.academic_service.entity.InvoiceStatus.CANCELLED
    """)
    List<Long> findEnrollmentsAlreadyInvoiced(
            @Param("period") LocalDate period,
            @Param("enrollmentIds") List<Long> enrollmentIds);

    /**
     * Invoice search with student-level filters resolved through an enrollment
     * subquery (Invoice carries enrollmentId only, no FK relationship to
     * Enrollment, so we filter the enrollmentId set directly).
     *
     * All filter parameters are nullable — pass null to skip a filter.
     * studentSearch matches case-insensitively against student_system_id OR
     * the student's English name.
     */
    @Query("""
        SELECT i FROM Invoice i
        WHERE (:enrollmentId IS NULL OR i.enrollmentId = :enrollmentId)
          AND (:period IS NULL OR i.billingPeriod = :period)
          AND (:status IS NULL OR i.status = :status)
          AND (:invoiceNumber IS NULL
               OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :invoiceNumber, '%')))
          AND (
               (:classId IS NULL AND :academicYearId IS NULL
                AND :shiftId IS NULL AND :genderSectionId IS NULL
                AND :studentSearch IS NULL)
            OR i.enrollmentId IN (
                  SELECT e.id FROM Enrollment e
                  WHERE (:classId IS NULL OR e.studentClass.id = :classId)
                    AND (:academicYearId IS NULL OR e.academicYear.id = :academicYearId)
                    AND (:shiftId IS NULL OR e.shift.id = :shiftId)
                    AND (:genderSectionId IS NULL OR e.genderSection.id = :genderSectionId)
                    AND (
                          :studentSearch IS NULL
                       OR LOWER(e.studentSystemId) LIKE LOWER(CONCAT('%', :studentSearch, '%'))
                       OR (e.student IS NOT NULL AND LOWER(e.student.nameEnglish) LIKE LOWER(CONCAT('%', :studentSearch, '%')))
                       OR (e.student IS NOT NULL AND LOWER(e.student.nameBangla)  LIKE LOWER(CONCAT('%', :studentSearch, '%')))
                       )
              )
          )
        ORDER BY i.billingPeriod DESC, i.id DESC
    """)
    Page<Invoice> search(
            @Param("enrollmentId") Long enrollmentId,
            @Param("period") LocalDate period,
            @Param("status") InvoiceStatus status,
            @Param("classId") Integer classId,
            @Param("academicYearId") Integer academicYearId,
            @Param("shiftId") Integer shiftId,
            @Param("genderSectionId") Integer genderSectionId,
            @Param("studentSearch") String studentSearch,
            @Param("invoiceNumber") String invoiceNumber,
            Pageable pageable);

    @Query("""
        SELECT i.invoiceNumber FROM Invoice i
        WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%')
        ORDER BY i.id DESC
    """)
    List<String> findRecentNumbersByPrefix(@Param("prefix") String prefix, Pageable pageable);
}
