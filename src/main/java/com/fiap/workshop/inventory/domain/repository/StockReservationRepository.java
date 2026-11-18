package com.fiap.workshop.inventory.domain.repository;

import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository {

    StockReservation save(StockReservation reservation);

    Optional<StockReservation> findByServiceOrderId(UUID serviceOrderId);
}
