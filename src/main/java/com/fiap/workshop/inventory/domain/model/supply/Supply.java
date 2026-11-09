package com.fiap.workshop.inventory.domain.model.supply;

import com.fiap.workshop.inventory.domain.model.shared.Money;

import java.util.UUID;

public class Supply {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private SupplyType type;
    private Money unitPrice;
    private int stockQuantity;
    private int minimumStock;

    public Supply(UUID id, String code, String name, String description, SupplyType type,
                  Money unitPrice, int stockQuantity, int minimumStock) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.type = type;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.minimumStock = minimumStock;
    }

    public static Supply create(String code, String name, String description, SupplyType type,
                                Money unitPrice, int minimumStock) {
        return new Supply(UUID.randomUUID(), code, name, description, type, unitPrice, 0, minimumStock);
    }

    public void update(String name, String description, SupplyType type, Money unitPrice, int minimumStock) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.unitPrice = unitPrice;
        this.minimumStock = minimumStock;
    }

    public void deductStock(int quantity) {
        this.stockQuantity -= quantity;
    }

    public void addStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.stockQuantity += quantity;
    }

    public boolean isBelowMinimum() {
        return this.stockQuantity < this.minimumStock;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public SupplyType getType() { return type; }
    public Money getUnitPrice() { return unitPrice; }
    public int getStockQuantity() { return stockQuantity; }
    public int getMinimumStock() { return minimumStock; }
}
