package com.securetravels.crm.basecamp;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "company_settings")
public class CompanySetting extends Auditable {

    @Id
    @Column(name = "setting_key", length = 80)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "text")
    private String settingValue;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected CompanySetting() {}

    public CompanySetting(String settingKey, String settingValue) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public String getSettingKey() { return settingKey; }
    public String getSettingValue() { return settingValue; }
    public UUID getUpdatedBy() { return updatedBy; }

    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
