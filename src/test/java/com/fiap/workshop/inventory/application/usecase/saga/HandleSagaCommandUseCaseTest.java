package com.fiap.workshop.inventory.application.usecase.saga;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.port.in.reservation.ConsumeStockInputPort;
import com.fiap.workshop.inventory.application.port.in.reservation.ReleaseStockInputPort;
import com.fiap.workshop.inventory.application.port.in.reservation.ReserveStockInputPort;
import com.fiap.workshop.inventory.application.port.out.SagaEventPublisher;
import com.fiap.workshop.inventory.application.saga.command.ConsumeStock;
import com.fiap.workshop.inventory.application.saga.command.ReleaseStock;
import com.fiap.workshop.inventory.application.saga.command.ReserveStock;
import com.fiap.workshop.inventory.application.saga.command.SagaCommand;
import com.fiap.workshop.inventory.application.saga.event.SagaEvent;
import com.fiap.workshop.inventory.application.saga.event.StockConsumed;
import com.fiap.workshop.inventory.application.saga.event.StockReleased;
import com.fiap.workshop.inventory.application.saga.event.StockReservationFailed;
import com.fiap.workshop.inventory.application.saga.event.StockReserved;
import com.fiap.workshop.inventory.domain.exception.InsufficientStockException;
import com.fiap.workshop.inventory.domain.exception.InvalidStateException;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleSagaCommandUseCaseTest {

    @Mock
    private ReserveStockInputPort reserveStock;
    @Mock
    private ReleaseStockInputPort releaseStock;
    @Mock
    private ConsumeStockInputPort consumeStock;
    @Mock
    private SagaEventPublisher publisher;

    private HandleSagaCommandUseCase useCase;

    private final UUID messageId = UUID.randomUUID();
    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID supplyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new HandleSagaCommandUseCase(reserveStock, releaseStock, consumeStock, publisher);
    }

    private SagaEvent published() {
        ArgumentCaptor<SagaEvent> captor = ArgumentCaptor.forClass(SagaEvent.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }

    private ReserveStock reserveCommand() {
        return new ReserveStock(sagaId, orderId, List.of(new ReserveStock.Line(supplyId, 2)));
    }

    @Test
    void shouldReserveAndAnswerReserved() {
        useCase.handle(messageId, reserveCommand());

        verify(reserveStock).execute(any());
        StockReserved event = (StockReserved) published();
        assertEquals(sagaId, event.sagaId());
        assertEquals(orderId, event.serviceOrderId());
    }

    @Test
    void shouldAnswerFailedWithTheReasonWhenThereIsNotEnoughStock() {
        when(reserveStock.execute(any())).thenThrow(new InsufficientStockException(supplyId, "Filter", 2, 1));

        useCase.handle(messageId, reserveCommand());

        StockReservationFailed event = (StockReservationFailed) published();
        assertEquals(true, event.reason().contains("Insufficient stock of 'Filter'"));
    }

    @Test
    void shouldAnswerFailedWhenASupplyDoesNotExist() {
        when(reserveStock.execute(any())).thenThrow(new ResourceNotFoundException("Supply", supplyId));

        useCase.handle(messageId, reserveCommand());

        assertEquals(StockReservationFailed.class, published().getClass());
    }

    @Test
    void shouldReleaseAndConfirm() {
        useCase.handle(messageId, new ReleaseStock(sagaId, orderId));

        verify(releaseStock).execute(orderId);
        assertEquals(StockReleased.class, published().getClass());
    }

    @Test
    void shouldConsumeAndConfirm() {
        when(consumeStock.execute(orderId)).thenReturn(org.mockito.Mockito.mock(ReservationResponse.class));

        useCase.handle(messageId, new ConsumeStock(sagaId, orderId));

        assertEquals(StockConsumed.class, published().getClass());
    }

    @Test
    void shouldNotAnswerWhenAnUnexpectedErrorOccurs() {
        doThrow(new InvalidStateException("consumed")).when(releaseStock).execute(orderId);

        assertThrows(InvalidStateException.class, () -> useCase.handle(messageId, new ReleaseStock(sagaId, orderId)));
        verify(publisher, never()).publish(any());
    }

    @Test
    void shouldDeriveTheAnswerIdFromTheCommandMessageId() {
        useCase.handle(messageId, new ReleaseStock(sagaId, orderId));
        SagaEvent first = published();
        clearInvocations(publisher);
        useCase.handle(messageId, new ReleaseStock(sagaId, orderId));
        SagaEvent repeat = published();
        clearInvocations(publisher);
        useCase.handle(UUID.randomUUID(), new ReleaseStock(sagaId, orderId));
        SagaEvent other = published();

        assertEquals(first.messageId(), repeat.messageId());
        assertNotEquals(first.messageId(), other.messageId());
    }

    @Test
    void shouldRejectACommandItDoesNotKnow() {
        SagaCommand unknown = new SagaCommand() {
            public UUID sagaId() { return sagaId; }
            public UUID serviceOrderId() { return orderId; }
        };

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(messageId, unknown));
        verifyNoInteractions(reserveStock, releaseStock, consumeStock, publisher);
    }
}
