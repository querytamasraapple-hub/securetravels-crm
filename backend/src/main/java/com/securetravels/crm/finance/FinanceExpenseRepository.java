package com.securetravels.crm.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanceExpenseRepository extends JpaRepository<FinanceExpense, UUID> {

    List<FinanceExpense> findByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate from, LocalDate to);

    List<FinanceExpense> findAllByOrderByExpenseDateDesc();
}
