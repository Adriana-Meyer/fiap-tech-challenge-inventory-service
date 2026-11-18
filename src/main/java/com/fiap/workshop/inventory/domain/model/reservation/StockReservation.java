package com.fiap.workshop.inventory.domain.model.reservation;

import com.fiap.workshop.inventory.domain.exception.InvalidStateException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The supplies held for one service order. The stock is taken off the shelf
 * when the reservation is made; releasing gives it back and consuming keeps it
 * out for good. An order with no supplies has an empty reservation, so the
 * saga treats it like any other.
 */
public class StockReservation {

    private final UUID id;
    private final UUID serviceOrderId;
    private final List<ReservedItem> items;
    private final Instant createdAt;
    private ReservationStatus status;
    private Instant updatedAt;

    public StockReservation(UUID id, UUID serviceOrderId, List<ReservedItem> items, ReservationStatus status,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.items = List.copyOf(items);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Lines for the same supply are added up. */
    public static StockReservation reserve(UUID serviceOrderId, List<ReservedItem> requested) {
        if (serviceOrderId == null) {
            throw new IllegalArgumentException("Service order id is required");
        }
        Map<UUID, Integer> bySupply = new LinkedHashMap<>();
        requested.forEach(item -> bySupply.merge(item.supplyId(), item.quantity(), Integer::sum));
        List<ReservedItem> merged = new ArrayList<>();
        bySupply.forEach((supplyId, quantity) -> merged.add(new ReservedItem(supplyId, quantity)));
        Instant now = Instant.now();
        return new StockReservation(UUID.randomUUID(), serviceOrderId, merged, ReservationStatus.RESERVED, now, now);
    }

    public boolean isReserved() {
        return status == ReservationStatus.RESERVED;
    }

    public void release() {
        if (status != ReservationStatus.RESERVED) {
            throw new InvalidStateException(
                    "Reservation of service order " + serviceOrderId + " cannot be released in status " + status);
        }
        this.status = ReservationStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    public void consume() {
        if (status != ReservationStatus.RESERVED) {
            throw new InvalidStateException(
                    "Reservation of service order " + serviceOrderId + " cannot be consumed in status " + status);
        }
        this.status = ReservationStatus.CONSUMED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getServiceOrderId() { return serviceOrderId; }
    public List<ReservedItem> getItems() { return items; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
