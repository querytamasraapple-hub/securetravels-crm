package com.securetravels.crm.basecamp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffLeaveRequestRepository extends JpaRepository<StaffLeaveRequest, UUID> {
    List<StaffLeaveRequest> findAllByOrderByCreatedAtDesc();
    List<StaffLeaveRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
