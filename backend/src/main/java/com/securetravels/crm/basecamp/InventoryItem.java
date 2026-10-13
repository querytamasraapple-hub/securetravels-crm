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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem extends Auditable {

    public enum Category { GEAR, SAFETY, CAMPING, MEDICAL, OFFICE, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category = Category.GEAR;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String unit = "NOS";

    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 2)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(length = 120)
    private String location;

    @Column(nullable = false)
    private boolean active = true;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    protected InventoryItem() {}

    public InventoryItem(String sku, String name, Category category) {
        this.sku = sku;
        this.name = name;
        this.category = category;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public String getLocation() { return location; }
    public boolean isActive() { return active; }
    public String getNotes() { return notes; }
    public UUID getRecordedBy() { return recordedBy; }

    public void setName(String name) { this.name = name; }
    public void setCategory(Category category) { this.category = category; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }
    public void setLocation(String location) { this.location = location; }
    public void setActive(boolean active) { this.active = active; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}
