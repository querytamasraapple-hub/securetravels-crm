package com.securetravels.crm.accounts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountCommissionPayableRepository extends JpaRepository<AccountCommissionPayable, UUID> {

    Optional<AccountCommissionPayable> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    List<AccountCommissionPayable> findByAccountIdOrderByPayableAtDesc(UUID accountId);

    List<AccountCommissionPayable> findByAccountIdAndStatus(UUID accountId, AccountCommissionPayable.Status status);

    @Query("select coalesce(sum(p.commissionAmount), 0) from AccountCommissionPayable p "
            + "where p.accountId = :accountId and p.status = 'OPEN'")
    BigDecimal sumOpenByAccount(@Param("accountId") UUID accountId);

    @Query("select coalesce(sum(p.commissionAmount), 0) from AccountCommissionPayable p "
            + "where p.accountId = :accountId and p.status = 'PAID'")
    BigDecimal sumPaidByAccount(@Param("accountId") UUID accountId);
}