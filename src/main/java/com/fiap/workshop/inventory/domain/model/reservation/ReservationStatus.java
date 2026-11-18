package com.fiap.workshop.inventory.domain.model.reservation;

public enum ReservationStatus {
    /** The stock is held for the order. */
    RESERVED,
    /** The order was cancelled: the stock went back to the shelf. */
    RELEASED,
    /** The work is done: the stock is spent for good. */
    CONSUMED
}
