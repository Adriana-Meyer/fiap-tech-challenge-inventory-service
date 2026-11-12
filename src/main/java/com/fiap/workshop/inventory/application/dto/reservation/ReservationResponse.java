package com.fiap.workshop.inventory.application.dto.reservation;

import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID serviceOrderId,
        String status,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt
) {

    public record Item(UUID supplyId, int quantity) {}

    public static ReservationResponse from(StockReservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getServiceOrderId(),
                reservation.getStatus().name(),
                reservation.getItems().stream().map(i -> new Item(i.supplyId(), i.quantity())).toList(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
