package com.example.academic_service.repository;

import com.example.academic_service.entity.PaymentFailure;
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
public interface PaymentFailureRepository extends JpaRepository<PaymentFailure, Long> {

    Optional<PaymentFailure> findByTranId(String tranId);

    boolean existsByTranId(String tranId);

    List<PaymentFailure> findByInvoiceIdOrderByIdDesc(Long invoiceId);

    @Query("""
        SELECT f FROM PaymentFailure f
        WHERE (:status IS NULL OR f.status = :status)
          AND (:invoiceId IS NULL OR f.invoiceId = :invoiceId)
          AND (:from IS NULL OR f.failedAt >= :from)
          AND (:to   IS NULL OR f.failedAt <= :to)
        ORDER BY f.failedAt DESC, f.id DESC
    """)
    Page<PaymentFailure> search(
            @Param("status") PaymentStatus status,
            @Param("invoiceId") Long invoiceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
