package com.fiap.workshop.inventory.application.dto.supply;

import com.fiap.workshop.inventory.domain.model.supply.Supply;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplyResponse(
        UUID id,
        String code,
        String name,
        String description,
        String type,
        BigDecimal unitPrice,
        int stockQuantity,
        int minimumStock,
        boolean belowMinimum
) {
    public static SupplyResponse from(Supply supply) {
        return new SupplyResponse(
                supply.getId(),
                supply.getCode(),
                supply.getName(),
                supply.getDescription(),
                supply.getType().name(),
                supply.getUnitPrice().getAmount(),
                supply.getStockQuantity(),
                supply.getMinimumStock(),
                supply.isBelowMinimum()
        );
    }
}
