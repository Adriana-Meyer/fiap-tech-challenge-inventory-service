package com.fiap.workshop.inventory.domain.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID supplyId, String supplyName, int requested, int available) {
        super("Insufficient stock of '" + supplyName + "' (" + supplyId + "): requested " + requested
                + ", available " + available);
    }
}
