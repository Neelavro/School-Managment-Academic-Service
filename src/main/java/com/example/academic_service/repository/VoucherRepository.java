package com.example.academic_service.repository;

import com.example.academic_service.entity.Voucher;
import com.example.academic_service.entity.VoucherType;
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
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByVoucherNumber(String voucherNumber);

    @Query("""
        SELECT v FROM Voucher v
        WHERE (:type IS NULL OR v.voucherType = :type)
          AND (:from IS NULL OR v.voucherDate >= :from)
          AND (:to   IS NULL OR v.voucherDate <= :to)
        ORDER BY v.voucherDate DESC, v.id DESC
    """)
    Page<Voucher> search(
            @Param("type") VoucherType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
        SELECT v.voucherNumber FROM Voucher v
        WHERE v.voucherNumber LIKE CONCAT(:prefix, '%')
        ORDER BY v.id DESC
    """)
    List<String> findRecentNumbersByPrefix(@Param("prefix") String prefix, Pageable pageable);
}
