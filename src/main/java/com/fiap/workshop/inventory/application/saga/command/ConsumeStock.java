package com.fiap.workshop.inventory.application.saga.command;

import java.util.UUID;

/** The work is done: turn the reservation into consumption (the pivot step, never undone). */
public record ConsumeStock(UUID sagaId, UUID serviceOrderId) implements SagaCommand {}
