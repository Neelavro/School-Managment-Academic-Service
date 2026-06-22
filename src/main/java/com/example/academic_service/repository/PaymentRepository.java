package com.example.academic_service.repository;

import com.example.academic_service.entity.Payment;
import com.example.academic_service.entity.PaymentMethod;
import com.example.academic_service.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTranId(String tranId);

    List<Payment> findByInvoiceIdOrderByIdDesc(Long invoiceId);

    @Query("""
        SELECT p FROM Payment p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:method IS NULL OR p.method = :method)
          AND (:invoiceId IS NULL OR p.invoiceId = :invoiceId)
          AND (:from IS NULL OR p.initiatedAt >= :from)
          AND (:to   IS NULL OR p.initiatedAt <= :to)
        ORDER BY p.initiatedAt DESC, p.id DESC
    """)
    Page<Payment> search(
            @Param("status") PaymentStatus status,
            @Param("method") PaymentMethod method,
            @Param("invoiceId") Long invoiceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
