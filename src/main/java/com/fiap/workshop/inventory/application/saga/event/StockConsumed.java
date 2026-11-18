package com.fiap.workshop.inventory.application.saga.event;

import java.util.UUID;

public record StockConsumed(UUID messageId, UUID sagaId, UUID serviceOrderId) implements SagaEvent {}
