package com.securetravels.crm.basecamp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, UUID> {
    List<StaffAttendance> findByWorkDateBetweenOrderByWorkDateDesc(LocalDate from, LocalDate to);
    Optional<StaffAttendance> findByUserIdAndWorkDate(UUID userId, LocalDate workDate);
}
