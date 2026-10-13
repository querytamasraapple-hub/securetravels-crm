package com.securetravels.crm.finance;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceInvoiceRepository extends JpaRepository<FinanceInvoice, UUID> {

    @EntityGraph(attributePaths = "lines")
    @Query("select i from FinanceInvoice i order by i.issueDate desc, i.createdAt desc")
    List<FinanceInvoice> findAllWithLines();

    @EntityGraph(attributePaths = "lines")
    @Query("select i from FinanceInvoice i where i.id = :id")
    Optional<FinanceInvoice> findDetailedById(@Param("id") UUID id);

    Optional<FinanceInvoice> findByInvoiceNoIgnoreCase(String invoiceNo);

    long countByInvoiceNoStartingWith(String prefix);
}
