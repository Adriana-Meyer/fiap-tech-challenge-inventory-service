package com.fiap.workshop.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.workshop.inventory.application.saga.event.StockReleased;
import com.fiap.workshop.inventory.application.saga.event.StockReservationFailed;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SnsSagaEventPublisherTest {

    private static final String TOPIC = "arn:aws:sns:us-east-1:000000000000:saga-events";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SnsClient snsClient = mock(SnsClient.class);
    private final UUID messageId = UUID.randomUUID();
    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private PublishRequest sent() {
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldPublishTheEnvelopeWithOnlyTheEventFieldsInThePayload() throws Exception {
        new SnsSagaEventPublisher(snsClient, objectMapper, TOPIC)
                .publish(new StockReservationFailed(messageId, sagaId, orderId, "no stock"));

        PublishRequest request = sent();
        JsonNode body = objectMapper.readTree(request.message());
        assertEquals(TOPIC, request.topicArn());
        assertEquals(messageId.toString(), body.get("messageId").asText());
        assertEquals(sagaId.toString(), body.get("sagaId").asText());
        assertEquals(orderId.toString(), body.get("serviceOrderId").asText());
        assertEquals("StockReservationFailed", body.get("type").asText());
        assertEquals("no stock", body.get("payload").get("reason").asText());
        assertFalse(body.get("payload").has("sagaId"));
        assertFalse(body.get("payload").has("messageId"));
        assertEquals("StockReservationFailed", request.messageAttributes().get("type").stringValue());
    }

    @Test
    void shouldFailLoudlyWhenTheTopicIsNotConfigured() {
        SnsSagaEventPublisher publisher = new SnsSagaEventPublisher(snsClient, objectMapper, "");

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new StockReleased(messageId, sagaId, orderId)));
        verifyNoInteractions(snsClient);
    }
}
