package com.fiap.workshop.inventory.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.workshop.inventory.domain.model.reservation.ReservationStatus;
import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.domain.model.shared.Money;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import com.fiap.workshop.inventory.domain.repository.StockReservationRepository;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the service against a real SQS and SNS (LocalStack) and a real
 * PostgreSQL: a command is sent to the {@code inventory-commands} queue exactly
 * as the OS service would send it, and the answer is read from a queue
 * subscribed to the {@code saga-events} topic, as the OS service reads it.
 */
@SpringBootTest
@DisplayName("Saga messaging (integration)")
class SagaMessagingIntegrationTest {

    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory_db").withUsername("inventory").withPassword("inventory123");

    @SuppressWarnings("resource")
    static final LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"))
                    .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.SNS);

    private static final String COMMANDS_QUEUE = "inventory-commands";
    private static final String DEAD_LETTERS_QUEUE = "inventory-commands-dlq";
    private static final String TEST_EVENTS_QUEUE = "test-saga-events";

    private static SqsClient sqs;
    private static String topicArn;
    private static String commandsUrl;
    private static String deadLettersUrl;
    private static String eventsUrl;

    static {
        postgres.start();
        localstack.start();
        createInfrastructure();
    }

    /** What the infrastructure repositories will create: queue + DLQ, topic, and a queue subscribed to the topic. */
    private static void createInfrastructure() {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
        Region region = Region.of(localstack.getRegion());
        sqs = SqsClient.builder().endpointOverride(localstack.getEndpoint())
                .credentialsProvider(credentials).region(region).build();
        SnsClient sns = SnsClient.builder().endpointOverride(localstack.getEndpoint())
                .credentialsProvider(credentials).region(region).build();

        deadLettersUrl = sqs.createQueue(r -> r.queueName(DEAD_LETTERS_QUEUE)).queueUrl();
        String deadLettersArn = arnOf(deadLettersUrl);
        commandsUrl = sqs.createQueue(r -> r.queueName(COMMANDS_QUEUE).attributes(Map.of(
                QueueAttributeName.VISIBILITY_TIMEOUT, "2",
                QueueAttributeName.REDRIVE_POLICY,
                "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"2\"}".formatted(deadLettersArn)))).queueUrl();

        topicArn = sns.createTopic(r -> r.name("saga-events")).topicArn();
        eventsUrl = sqs.createQueue(r -> r.queueName(TEST_EVENTS_QUEUE)).queueUrl();
        sns.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(arnOf(eventsUrl))
                .attributes(Map.of("RawMessageDelivery", "true")));
    }

    private static String arnOf(String queueUrl) {
        return sqs.getQueueAttributes(r -> r.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
                .attributes().get(QueueAttributeName.QUEUE_ARN);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("spring.cloud.aws.sns.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        registry.add("app.messaging.events-topic-arn", () -> topicArn);
            }

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StockReservationRepository reservationRepository;
    @Autowired
    private SupplyRepository supplyRepository;
    @Autowired
    private TransactionTemplate transaction;

    private final List<JsonNode> received = new ArrayList<>();

    // ---- helpers ----------------------------------------------------------

    private void send(UUID messageId, UUID sagaId, UUID orderId, String type, String payload) {
        String body = """
                {"messageId":"%s","sagaId":"%s","serviceOrderId":"%s","type":"%s","payload":%s}
                """.formatted(messageId, sagaId, orderId, type, payload);
        sqs.sendMessage(r -> r.queueUrl(commandsUrl).messageBody(body));
    }

    private void send(UUID sagaId, UUID orderId, String type, String payload) {
        send(UUID.randomUUID(), sagaId, orderId, type, payload);
    }

    /** Waits for an answer about this order of this type, keeping the others it reads along the way. */
    private JsonNode awaitEvent(UUID orderId, String type) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            for (JsonNode event : received) {
                if (matches(event, orderId, type)) {
                    return event;
                }
            }
            for (Message message : sqs.receiveMessage(r -> r.queueUrl(eventsUrl).waitTimeSeconds(2)
                    .maxNumberOfMessages(10)).messages()) {
                received.add(objectMapper.readTree(message.body()));
                sqs.deleteMessage(r -> r.queueUrl(eventsUrl).receiptHandle(message.receiptHandle()));
            }
        }
        throw new AssertionError("No " + type + " for " + orderId + " within 30s; received " + received);
    }

    private static boolean matches(JsonNode event, UUID orderId, String type) {
        return orderId.toString().equals(event.get("serviceOrderId").asText()) && type.equals(event.get("type").asText());
    }

    /** The adapter maps lazily loaded lines, so the read needs a session: the use cases supply one, a test does not. */
    private StockReservation reservation(UUID orderId) {
        return transaction.execute(status -> reservationRepository.findByServiceOrderId(orderId).orElseThrow());
    }

    private Supply supplyWithStock(String code, int stock) {
        Supply supply = Supply.create(code + "-" + UUID.randomUUID().toString().substring(0, 8), code, "d",
                SupplyType.PART, Money.of(10), 1);
        supply.addStock(stock);
        return supplyRepository.save(supply);
    }

    private int stockOf(Supply supply) {
        return supplyRepository.findById(supply.getId()).orElseThrow().getStockQuantity();
    }

    private static String lines(Object... supplyAndQuantity) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < supplyAndQuantity.length; i += 2) {
            if (i > 0) items.append(",");
            items.append("{\"supplyId\":\"%s\",\"quantity\":%s}".formatted(
                    ((Supply) supplyAndQuantity[i]).getId(), supplyAndQuantity[i + 1]));
        }
        return "{\"items\":[" + items + "]}";
    }

    private long countEvents(UUID orderId, String type) {
        return received.stream().filter(e -> matches(e, orderId, type)).count();
    }

    // ---- tests ------------------------------------------------------------

    @Test
    @DisplayName("should reserve the stock and answer reserved")
    void shouldReserveTheStockAndAnswerReserved() throws Exception {
        Supply oil = supplyWithStock("Oil", 10);
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        send(sagaId, orderId, "ReserveStock", lines(oil, 4));

        JsonNode event = awaitEvent(orderId, "StockReserved");
        assertEquals(sagaId.toString(), event.get("sagaId").asText());
        assertEquals(6, stockOf(oil));
        StockReservation reservation = reservation(orderId);
        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
        assertEquals(4, reservation.getItems().get(0).quantity());
    }

    @Test
    @DisplayName("should answer failed, not fail, when the stock is insufficient")
    void shouldAnswerFailedNotFailWhenTheStockIsInsufficient() throws Exception {
        Supply filter = supplyWithStock("Filter", 1);
        UUID orderId = UUID.randomUUID();

        send(UUID.randomUUID(), orderId, "ReserveStock", lines(filter, 3));

        JsonNode failed = awaitEvent(orderId, "StockReservationFailed");
        assertTrue(failed.get("payload").get("reason").asText().contains("Insufficient stock"));
        assertEquals(1, stockOf(filter));
        assertTrue(reservationRepository.findByServiceOrderId(orderId).isEmpty());
    }

    @Test
    @DisplayName("should reserve all the supplies or none")
    void shouldReserveAllTheSuppliesOrNone() throws Exception {
        Supply plenty = supplyWithStock("Plenty", 10);
        Supply scarce = supplyWithStock("Scarce", 1);
        UUID orderId = UUID.randomUUID();

        send(UUID.randomUUID(), orderId, "ReserveStock", lines(plenty, 5, scarce, 2));

        awaitEvent(orderId, "StockReservationFailed");
        assertEquals(10, stockOf(plenty));
        assertEquals(1, stockOf(scarce));
    }

    @Test
    @DisplayName("should answer failed when the supply does not exist")
    void shouldAnswerFailedWhenTheSupplyDoesNotExist() throws Exception {
        UUID orderId = UUID.randomUUID();

        send(UUID.randomUUID(), orderId, "ReserveStock",
                "{\"items\":[{\"supplyId\":\"%s\",\"quantity\":1}]}".formatted(UUID.randomUUID()));

        JsonNode failed = awaitEvent(orderId, "StockReservationFailed");
        assertTrue(failed.get("payload").get("reason").asText().contains("Supply not found"));
    }

    @Test
    @DisplayName("should reserve nothing for an order without supplies and still confirm")
    void shouldReserveNothingForAnOrderWithoutSuppliesAndStillConfirm() throws Exception {
        UUID orderId = UUID.randomUUID();

        send(UUID.randomUUID(), orderId, "ReserveStock", "{\"items\":[]}");

        awaitEvent(orderId, "StockReserved");
        assertEquals(0, reservation(orderId).getItems().size());
    }

    @Test
    @DisplayName("should give the stock back when released and confirm, also when repeated")
    void shouldGiveTheStockBackWhenReleasedAndConfirmAlsoWhenRepeated() throws Exception {
        Supply oil = supplyWithStock("Oil", 10);
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        send(sagaId, orderId, "ReserveStock", lines(oil, 4));
        awaitEvent(orderId, "StockReserved");

        send(sagaId, orderId, "ReleaseStock", "{}");
        awaitEvent(orderId, "StockReleased");
        assertEquals(10, stockOf(oil));
        assertEquals(ReservationStatus.RELEASED, reservation(orderId).getStatus());

        UUID repeatedOrder = orderId;
        send(sagaId, repeatedOrder, "ReleaseStock", "{}");
        long deadline = System.currentTimeMillis() + 15_000;
        while (countEvents(orderId, "StockReleased") < 2 && System.currentTimeMillis() < deadline) {
            awaitEventQuietly(orderId);
        }
        assertEquals(2, countEvents(orderId, "StockReleased"));
        assertEquals(10, stockOf(oil));
    }

    @Test
    @DisplayName("should confirm a release when nothing was reserved")
    void shouldConfirmAReleaseWhenNothingWasReserved() throws Exception {
        UUID orderId = UUID.randomUUID();

        send(UUID.randomUUID(), orderId, "ReleaseStock", "{}");

        awaitEvent(orderId, "StockReleased");
    }

    @Test
    @DisplayName("should consume the reservation keeping the stock off the shelf")
    void shouldConsumeTheReservationKeepingTheStockOffTheShelf() throws Exception {
        Supply oil = supplyWithStock("Oil", 10);
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        send(sagaId, orderId, "ReserveStock", lines(oil, 4));
        awaitEvent(orderId, "StockReserved");

        send(sagaId, orderId, "ConsumeStock", "{}");

        awaitEvent(orderId, "StockConsumed");
        assertEquals(ReservationStatus.CONSUMED, reservation(orderId).getStatus());
        assertEquals(6, stockOf(oil));
    }

    @Test
    @DisplayName("should answer a repeated reservation with the same message id and deduct only once")
    void shouldAnswerARepeatedReservationWithTheSameMessageIdAndDeductOnlyOnce() throws Exception {
        Supply oil = supplyWithStock("Oil", 10);
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();

        send(commandId, sagaId, orderId, "ReserveStock", lines(oil, 4));
        send(commandId, sagaId, orderId, "ReserveStock", lines(oil, 4));
        JsonNode first = awaitEvent(orderId, "StockReserved");
        long deadline = System.currentTimeMillis() + 15_000;
        while (countEvents(orderId, "StockReserved") < 2 && System.currentTimeMillis() < deadline) {
            awaitEventQuietly(orderId);
        }

        assertEquals(2, countEvents(orderId, "StockReserved"));
        List<String> ids = received.stream().filter(e -> matches(e, orderId, "StockReserved"))
                .map(e -> e.get("messageId").asText()).distinct().toList();
        assertEquals(List.of(first.get("messageId").asText()), ids);
        assertEquals(6, stockOf(oil));
    }

    @Test
    @DisplayName("should move a message it cannot handle to the dead-letter queue")
    void shouldMoveAMessageItCannotHandleToTheDeadLetterQueue() {
        UUID orderId = UUID.randomUUID();
        // Consuming a reservation that was never made fails in the use case on every attempt.
        send(UUID.randomUUID(), orderId, "ConsumeStock", "{}");

        long deadline = System.currentTimeMillis() + 45_000;
        List<Message> dead = List.of();
        while (dead.isEmpty() && System.currentTimeMillis() < deadline) {
            dead = sqs.receiveMessage(r -> r.queueUrl(deadLettersUrl).waitTimeSeconds(2)).messages();
        }

        assertEquals(1, dead.size());
        assertTrue(dead.get(0).body().contains(orderId.toString()));
    }

    /** One short read of the events queue, only to let a late answer arrive. */
    private void awaitEventQuietly(UUID orderId) throws Exception {
        for (Message message : sqs.receiveMessage(r -> r.queueUrl(eventsUrl).waitTimeSeconds(2)
                .maxNumberOfMessages(10)).messages()) {
            received.add(objectMapper.readTree(message.body()));
            sqs.deleteMessage(r -> r.queueUrl(eventsUrl).receiptHandle(message.receiptHandle()));
        }
    }
}
