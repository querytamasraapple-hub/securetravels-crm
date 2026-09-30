package com.securetravels.crm.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByAccountTypeAndActiveOrderByNameAsc(Account.AccountType type, boolean active);

    List<Account> findByActiveOrderByNameAsc(boolean active);

    Optional<Account> findByIdAndActiveTrue(UUID id);

    Optional<Account> findByGstin(String normalizedGstin);
}