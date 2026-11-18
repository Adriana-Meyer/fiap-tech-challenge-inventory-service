package com.fiap.workshop.inventory.application.port.in.reservation;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;

import java.util.UUID;

public interface ConsumeStockInputPort {

    /** Turns the reservation into consumption. Idempotent. */
    ReservationResponse execute(UUID serviceOrderId);
}
