package com.securetravels.crm.basecamp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VisitorLogRepository extends JpaRepository<VisitorLog, UUID> {
    List<VisitorLog> findAllByOrderByCheckedInAtDesc();
}
