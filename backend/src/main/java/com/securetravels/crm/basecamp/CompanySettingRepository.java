package com.securetravels.crm.basecamp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanySettingRepository extends JpaRepository<CompanySetting, String> {
    List<CompanySetting> findAllByOrderBySettingKeyAsc();
}
