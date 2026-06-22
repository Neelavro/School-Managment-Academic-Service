package com.example.academic_service.repository;

import com.example.academic_service.entity.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    List<InvoiceLine> findByInvoiceId(Long invoiceId);
    List<InvoiceLine> findByInvoiceIdIn(List<Long> invoiceIds);
}
