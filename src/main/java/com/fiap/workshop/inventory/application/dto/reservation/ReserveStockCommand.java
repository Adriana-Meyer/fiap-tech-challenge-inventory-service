package com.fiap.workshop.inventory.application.dto.reservation;

import java.util.List;
import java.util.UUID;

public record ReserveStockCommand(UUID serviceOrderId, List<Line> items) {

    public record Line(UUID supplyId, int quantity) {}
}
