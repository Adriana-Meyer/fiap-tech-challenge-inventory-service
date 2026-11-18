package com.fiap.workshop.inventory.application.dto.supply;

import jakarta.validation.constraints.Positive;

public record StockAdjustmentCommand(
        @Positive int quantity
) {}
