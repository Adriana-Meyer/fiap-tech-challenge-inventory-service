package com.fiap.workshop.inventory.application.saga.event;

import java.util.UUID;

/** Compensation done. */
public record StockReleased(UUID messageId, UUID sagaId, UUID serviceOrderId) implements SagaEvent {}
