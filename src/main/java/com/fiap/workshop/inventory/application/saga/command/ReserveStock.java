package com.fiap.workshop.inventory.application.saga.command;

import java.util.List;
import java.util.UUID;

/** Reserve the supplies of the approved order; an order without supplies sends no lines. */
public record ReserveStock(UUID sagaId, UUID serviceOrderId, List<Line> items) implements SagaCommand {

    public record Line(UUID supplyId, int quantity) {}
}
