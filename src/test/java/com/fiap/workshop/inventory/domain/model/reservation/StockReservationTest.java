package com.fiap.workshop.inventory.domain.model.reservation;

import com.fiap.workshop.inventory.domain.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockReservationTest {

    private final UUID orderId = UUID.randomUUID();
    private final UUID oil = UUID.randomUUID();
    private final UUID filter = UUID.randomUUID();

    @Test
    void shouldReserveAndAddUpLinesOfTheSameSupply() {
        StockReservation reservation = StockReservation.reserve(orderId, List.of(
                new ReservedItem(oil, 2), new ReservedItem(filter, 1), new ReservedItem(oil, 3)));

        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
        assertTrue(reservation.isReserved());
        assertEquals(List.of(new ReservedItem(oil, 5), new ReservedItem(filter, 1)), reservation.getItems());
    }

    @Test
    void shouldAllowAReservationWithNoLines() {
        assertTrue(StockReservation.reserve(orderId, List.of()).getItems().isEmpty());
    }

    @Test
    void shouldRejectAMissingOrderOrAnInvalidLine() {
        assertThrows(IllegalArgumentException.class, () -> StockReservation.reserve(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ReservedItem(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ReservedItem(oil, 0));
    }

    @Test
    void shouldReleaseOnlyWhenReserved() {
        StockReservation reservation = StockReservation.reserve(orderId, List.of(new ReservedItem(oil, 1)));
        reservation.release();

        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        assertFalse(reservation.isReserved());
        assertThrows(InvalidStateException.class, reservation::release);
        assertThrows(InvalidStateException.class, reservation::consume);
    }

    @Test
    void shouldConsumeOnlyWhenReserved() {
        StockReservation reservation = StockReservation.reserve(orderId, List.of(new ReservedItem(oil, 1)));
        reservation.consume();

        assertEquals(ReservationStatus.CONSUMED, reservation.getStatus());
        assertThrows(InvalidStateException.class, reservation::consume);
        assertThrows(InvalidStateException.class, reservation::release);
    }
}
