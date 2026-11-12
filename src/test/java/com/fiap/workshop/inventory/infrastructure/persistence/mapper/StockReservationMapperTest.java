package com.fiap.workshop.inventory.infrastructure.persistence.mapper;

import com.fiap.workshop.inventory.domain.model.reservation.ReservedItem;
import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockReservationMapperTest {

    private final StockReservationMapper mapper = new StockReservationMapper();

    @Test
    void shouldRoundTripAReservationKeepingTheLineOrder() {
        StockReservation reservation = StockReservation.reserve(UUID.randomUUID(), List.of(
                new ReservedItem(UUID.randomUUID(), 2), new ReservedItem(UUID.randomUUID(), 5)));
        reservation.consume();

        StockReservationJpaEntity entity = mapper.toEntity(reservation);
        StockReservation back = mapper.toDomain(entity);

        assertEquals(0, entity.getItems().get(0).getPosition());
        assertEquals(1, entity.getItems().get(1).getPosition());
        assertEquals(reservation.getId(), back.getId());
        assertEquals(reservation.getServiceOrderId(), back.getServiceOrderId());
        assertEquals(reservation.getItems(), back.getItems());
        assertEquals(reservation.getStatus(), back.getStatus());
        assertEquals(reservation.getCreatedAt(), back.getCreatedAt());
    }
}
