package com.fiap.workshop.inventory.application.dto.supply;

import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpsertSupplyCommand(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull SupplyType type,
        @NotNull @Positive BigDecimal unitPrice,
        @Min(0) int minimumStock
) {}
