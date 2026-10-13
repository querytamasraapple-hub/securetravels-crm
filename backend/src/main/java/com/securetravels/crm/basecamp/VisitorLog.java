package com.securetravels.crm.basecamp;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visitor_logs")
public class VisitorLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "visitor_name", nullable = false, length = 160)
    private String visitorName;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 200)
    private String purpose;

    @Column(name = "host_user_id")
    private UUID hostUserId;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt = Instant.now();

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    protected VisitorLog() {}

    public VisitorLog(String visitorName, String purpose) {
        this.visitorName = visitorName;
        this.purpose = purpose;
    }

    public UUID getId() { return id; }
    public String getVisitorName() { return visitorName; }
    public String getPhone() { return phone; }
    public String getPurpose() { return purpose; }
    public UUID getHostUserId() { return hostUserId; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public Instant getCheckedOutAt() { return checkedOutAt; }
    public String getNotes() { return notes; }
    public UUID getRecordedBy() { return recordedBy; }

    public void setPhone(String phone) { this.phone = phone; }
    public void setHostUserId(UUID hostUserId) { this.hostUserId = hostUserId; }
    public void setCheckedOutAt(Instant checkedOutAt) { this.checkedOutAt = checkedOutAt; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}
