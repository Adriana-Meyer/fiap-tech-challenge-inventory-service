package com.fiap.workshop.inventory.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "stock_reservation_items")
public class StockReservationItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "supply_id", nullable = false)
    private UUID supplyId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected StockReservationItemJpaEntity() {}

    public StockReservationItemJpaEntity(UUID id, int position, UUID supplyId, int quantity) {
        this.id = id;
        this.position = position;
        this.supplyId = supplyId;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public int getPosition() { return position; }
    public UUID getSupplyId() { return supplyId; }
    public int getQuantity() { return quantity; }
}
