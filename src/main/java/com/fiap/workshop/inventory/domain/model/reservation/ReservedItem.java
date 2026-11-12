package com.fiap.workshop.inventory.domain.model.reservation;

import java.util.UUID;

public record ReservedItem(UUID supplyId, int quantity) {

    public ReservedItem {
        if (supplyId == null) {
            throw new IllegalArgumentException("Supply id is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reserved quantity must be positive");
        }
    }
}
