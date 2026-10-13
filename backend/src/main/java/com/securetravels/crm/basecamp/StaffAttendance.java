package com.securetravels.crm.basecamp;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "work_date"}))
public class StaffAttendance extends Auditable {

    public enum Status { PRESENT, ABSENT, HALF_DAY, ON_LEAVE, WFH }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PRESENT;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    protected StaffAttendance() {}

    public StaffAttendance(UUID userId, LocalDate workDate, Status status) {
        this.userId = userId;
        this.workDate = workDate;
        this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getWorkDate() { return workDate; }
    public Instant getCheckInAt() { return checkInAt; }
    public Instant getCheckOutAt() { return checkOutAt; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
    public UUID getRecordedBy() { return recordedBy; }

    public void setCheckInAt(Instant checkInAt) { this.checkInAt = checkInAt; }
    public void setCheckOutAt(Instant checkOutAt) { this.checkOutAt = checkOutAt; }
    public void setStatus(Status status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}
