package com.securetravels.crm.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountInvoiceRepository extends JpaRepository<AccountInvoice, UUID> {

    Optional<AccountInvoice> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    List<AccountInvoice> findByAccountIdOrderByIssuedAtDesc(UUID accountId);

    Optional<AccountInvoice> findFirstByInvoiceRefStartingWithOrderByInvoiceRefDesc(String prefix);
}