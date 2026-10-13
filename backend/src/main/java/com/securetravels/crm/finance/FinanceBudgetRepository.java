package com.securetravels.crm.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceBudgetRepository extends JpaRepository<FinanceBudget, UUID> {
    List<FinanceBudget> findByPeriodMonthOrderByCategoryAsc(String periodMonth);
    Optional<FinanceBudget> findByPeriodMonthAndCategory(String periodMonth, String category);
}
