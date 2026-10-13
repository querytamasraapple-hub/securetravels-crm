package com.securetravels.crm.basecamp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    List<InventoryItem> findByActiveTrueOrderByNameAsc();
    boolean existsBySkuIgnoreCase(String sku);
}
