package com.fiap.workshop.inventory.application.saga.command;

import java.util.UUID;

/** Compensation: give the reserved stock back. */
public record ReleaseStock(UUID sagaId, UUID serviceOrderId) implements SagaCommand {}
