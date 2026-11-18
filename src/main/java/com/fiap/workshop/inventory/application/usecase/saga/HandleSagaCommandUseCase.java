package com.fiap.workshop.inventory.application.usecase.saga;

import com.fiap.workshop.inventory.application.dto.reservation.ReserveStockCommand;
import com.fiap.workshop.inventory.application.port.in.reservation.ConsumeStockInputPort;
import com.fiap.workshop.inventory.application.port.in.reservation.ReleaseStockInputPort;
import com.fiap.workshop.inventory.application.port.in.reservation.ReserveStockInputPort;
import com.fiap.workshop.inventory.application.port.in.saga.HandleSagaCommandInputPort;
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
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Turns each saga command into the use case that does the work and publishes
 * the matching answer. Not having the stock (or a supply) is an answer
 * ({@link StockReservationFailed}), because the saga reacts to it with
 * compensations; anything else unexpected is thrown so the message goes back
 * to the queue and, in the end, to the dead-letter queue.
 */
@Service
public class HandleSagaCommandUseCase implements HandleSagaCommandInputPort {

    private final ReserveStockInputPort reserveStock;
    private final ReleaseStockInputPort releaseStock;
    private final ConsumeStockInputPort consumeStock;
    private final SagaEventPublisher publisher;

    public HandleSagaCommandUseCase(ReserveStockInputPort reserveStock,
                                    ReleaseStockInputPort releaseStock,
                                    ConsumeStockInputPort consumeStock,
                                    SagaEventPublisher publisher) {
        this.reserveStock = reserveStock;
        this.releaseStock = releaseStock;
        this.consumeStock = consumeStock;
        this.publisher = publisher;
    }

    @Override
    public void handle(UUID messageId, SagaCommand command) {
        if (command instanceof ReserveStock c) {
            onReserveStock(messageId, c);
        } else if (command instanceof ReleaseStock c) {
            releaseStock.execute(c.serviceOrderId());
            publisher.publish(new StockReleased(answerId(messageId, StockReleased.class),
                    c.sagaId(), c.serviceOrderId()));
        } else if (command instanceof ConsumeStock c) {
            consumeStock.execute(c.serviceOrderId());
            publisher.publish(new StockConsumed(answerId(messageId, StockConsumed.class),
                    c.sagaId(), c.serviceOrderId()));
        } else {
            throw new IllegalArgumentException("Unsupported saga command " + command.getClass().getName());
        }
    }

    private void onReserveStock(UUID messageId, ReserveStock command) {
        try {
            reserveStock.execute(new ReserveStockCommand(command.serviceOrderId(), command.items().stream()
                    .map(line -> new ReserveStockCommand.Line(line.supplyId(), line.quantity())).toList()));
        } catch (InsufficientStockException | ResourceNotFoundException e) {
            publisher.publish(new StockReservationFailed(answerId(messageId, StockReservationFailed.class),
                    command.sagaId(), command.serviceOrderId(), e.getMessage()));
            return;
        }
        publisher.publish(new StockReserved(answerId(messageId, StockReserved.class),
                command.sagaId(), command.serviceOrderId()));
    }

    /** Same command message, same answer id: the orchestrator recognizes a repeat. */
    private static UUID answerId(UUID commandMessageId, Class<? extends SagaEvent> eventType) {
        return UUID.nameUUIDFromBytes(
                (commandMessageId + ":" + eventType.getSimpleName()).getBytes(StandardCharsets.UTF_8));
    }
}
