package com.securetravels.crm.basecamp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffPayrollRepository extends JpaRepository<StaffPayroll, UUID> {
    List<StaffPayroll> findByPeriodMonthOrderByCreatedAtDesc(String periodMonth);
    Optional<StaffPayroll> findByUserIdAndPeriodMonth(UUID userId, String periodMonth);
}
