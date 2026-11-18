package com.fiap.workshop.inventory.application.saga.event;

import java.util.UUID;

/** The reservation was refused, normally for lack of stock. Nothing was held. */
public record StockReservationFailed(UUID messageId, UUID sagaId, UUID serviceOrderId, String reason)
        implements SagaEvent {}
