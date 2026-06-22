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

    @Query("""
        SELECT i FROM Invoice i
        WHERE (:enrollmentId IS NULL OR i.enrollmentId = :enrollmentId)
          AND (:period IS NULL OR i.billingPeriod = :period)
          AND (:status IS NULL OR i.status = :status)
        ORDER BY i.billingPeriod DESC, i.id DESC
    """)
    Page<Invoice> search(
            @Param("enrollmentId") Long enrollmentId,
            @Param("period") LocalDate period,
            @Param("status") InvoiceStatus status,
            Pageable pageable);

    @Query("""
        SELECT i.invoiceNumber FROM Invoice i
        WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%')
        ORDER BY i.id DESC
    """)
    List<String> findRecentNumbersByPrefix(@Param("prefix") String prefix, Pageable pageable);
}
