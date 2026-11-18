package com.fiap.workshop.inventory.application.port.in.reservation;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.dto.reservation.ReserveStockCommand;

public interface ReserveStockInputPort {

    /**
     * Reserves all the supplies or none. Idempotent per service order: a
     * reservation that already exists is returned as it is.
     *
     * @throws com.fiap.workshop.inventory.domain.exception.InsufficientStockException when a supply lacks stock
     * @throws com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException when a supply does not exist
     */
    ReservationResponse execute(ReserveStockCommand command);
}
