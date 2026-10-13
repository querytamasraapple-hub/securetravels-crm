package com.securetravels.crm.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceVendorPayoutRepository extends JpaRepository<FinanceVendorPayout, UUID> {
    List<FinanceVendorPayout> findAllByOrderByPayoutDateDesc();
}
