package com.fiap.workshop.inventory.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.workshop.inventory.application.port.in.saga.HandleSagaCommandInputPort;
import com.fiap.workshop.inventory.application.saga.command.ConsumeStock;
import com.fiap.workshop.inventory.application.saga.command.ReleaseStock;
import com.fiap.workshop.inventory.application.saga.command.ReserveStock;
import com.fiap.workshop.inventory.application.saga.command.SagaCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SagaCommandListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HandleSagaCommandInputPort handler = mock(HandleSagaCommandInputPort.class);
    private final SagaCommandListener listener = new SagaCommandListener(objectMapper, handler);

    private final UUID messageId = UUID.randomUUID();
    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID supplyId = UUID.randomUUID();

    private String envelope(String type, String payload) {
        return """
                {"messageId":"%s","sagaId":"%s","serviceOrderId":"%s","type":"%s","payload":%s}
                """.formatted(messageId, sagaId, orderId, type, payload);
    }

    private SagaCommand handled() {
        ArgumentCaptor<SagaCommand> captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(handler).handle(org.mockito.ArgumentMatchers.eq(messageId), captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldHandleReserveStockMergingTheEnvelopeIds() throws Exception {
        listener.onMessage(envelope("ReserveStock", "{\"items\":[{\"supplyId\":\"%s\",\"quantity\":3}]}".formatted(supplyId)));

        ReserveStock command = (ReserveStock) handled();
        assertEquals(sagaId, command.sagaId());
        assertEquals(orderId, command.serviceOrderId());
        assertEquals(supplyId, command.items().get(0).supplyId());
        assertEquals(3, command.items().get(0).quantity());
    }

    @Test
    void shouldHandleAReservationOfAnOrderWithoutSupplies() throws Exception {
        listener.onMessage(envelope("ReserveStock", "{\"items\":[]}"));

        assertEquals(0, ((ReserveStock) handled()).items().size());
    }

    @Test
    void shouldHandleTheOtherCommandsWithAnEmptyOrMissingPayload() throws Exception {
        listener.onMessage(envelope("ReleaseStock", "{}"));
        assertEquals(ReleaseStock.class, handled().getClass());
        clearInvocations(handler);

        listener.onMessage(envelope("ConsumeStock", "null"));
        assertEquals(ConsumeStock.class, handled().getClass());
    }

    @Test
    void shouldFailOnAnUnknownTypeSoTheMessageGoesToTheDeadLetterQueue() {
        assertThrows(IllegalArgumentException.class, () -> listener.onMessage(envelope("GenerateBudget", "{}")));
        verifyNoInteractions(handler);
    }

    @Test
    void shouldFailOnAMessageThatIsNotJson() {
        assertThrows(JsonProcessingException.class, () -> listener.onMessage("not json"));
        verifyNoInteractions(handler);
    }

    @Test
    void shouldFailWhenTheHandlerFailsSoTheMessageIsRetried() {
        doThrow(new IllegalStateException("db down")).when(handler).handle(any(), any());

        assertThrows(IllegalStateException.class, () -> listener.onMessage(envelope("ReleaseStock", "{}")));
    }
}
