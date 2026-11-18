package com.fiap.workshop.inventory.integration;

import com.fiap.workshop.inventory.infrastructure.security.JwtTestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole service against a real PostgreSQL (Flyway migrations included)
 * and exercises security + REST + persistence together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.jwt.secret=" + JwtTestTokens.SECRET, "app.messaging.enabled=false"})
@DisplayName("Supply API (integration)")
class SupplyApiIntegrationTest {

    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("inventory")
            .withPassword("inventory123");

    static {
        postgres.start();
    }

    private static final String SEEDED_SUPPLY = "b2000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String bearer(String role) {
        return "Bearer " + JwtTestTokens.token("user-" + role, role);
    }

    private static String supplyJson(String code) {
        return """
                {"code":"%s","name":"Test Part","description":"d","type":"PART","unitPrice":19.90,"minimumStock":2}
                """.formatted(code);
    }

    @Test
    @DisplayName("should expose health and OpenAPI without a token")
    void shouldExposeHealthAndOpenApiWithoutAToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("should answer 401 without a token or with a token lacking the role claim")
    void shouldAnswer401WithoutATokenOrWithoutRoleClaim() throws Exception {
        mockMvc.perform(get("/api/v1/supplies")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/supplies")
                        .header("Authorization", "Bearer " + JwtTestTokens.token("u", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should list the seeded supplies for a mechanic")
    void shouldListTheSeededSuppliesForAMechanic() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/supplies").header("Authorization", bearer("MECHANIC")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode supplies = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(supplies.size() >= 12);
    }

    @Test
    @DisplayName("should forbid a mechanic from creating a supply")
    void shouldForbidAMechanicFromCreatingASupply() throws Exception {
        mockMvc.perform(post("/api/v1/supplies").header("Authorization", bearer("MECHANIC"))
                        .contentType(MediaType.APPLICATION_JSON).content(supplyJson("IT-FORBIDDEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should create, find, reject duplicate, update and delete a supply as admin")
    void shouldRunTheCrudLifecycleAsAdmin() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/supplies").header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(supplyJson("IT-CRUD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.belowMinimum").value(true))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/supplies").header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(supplyJson("IT-CRUD")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/supplies/{id}", id).header("Authorization", bearer("CONSULTANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("IT-CRUD"))
                .andExpect(jsonPath("$.unitPrice").value(19.90));

        mockMvc.perform(put("/api/v1/supplies/{id}", id)
                        .header("Authorization", bearer("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(supplyJson("IT-CRUD").replace("Test Part", "Renamed Part")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Part"));

        mockMvc.perform(delete("/api/v1/supplies/{id}", id).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/supplies/{id}", id).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should let a stockist add stock and persist the new quantity")
    void shouldLetAStockistAddStockAndPersistTheNewQuantity() throws Exception {
        MvcResult before = mockMvc.perform(get("/api/v1/supplies/{id}", SEEDED_SUPPLY)
                        .header("Authorization", bearer("STOCKIST"))).andReturn();
        int initial = objectMapper.readTree(before.getResponse().getContentAsString()).get("stockQuantity").asInt();

        mockMvc.perform(patch("/api/v1/supplies/{id}/stock", SEEDED_SUPPLY)
                        .header("Authorization", bearer("STOCKIST"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                .andExpect(status().isOk());

        MvcResult after = mockMvc.perform(get("/api/v1/supplies/{id}", SEEDED_SUPPLY)
                        .header("Authorization", bearer("STOCKIST"))).andReturn();
        assertEquals(initial + 5, objectMapper.readTree(after.getResponse().getContentAsString()).get("stockQuantity").asInt());
    }

    @Test
    @DisplayName("should answer 400 on invalid payload and 404 on unknown id")
    void shouldAnswer400OnInvalidPayloadAnd404OnUnknownId() throws Exception {
        mockMvc.perform(post("/api/v1/supplies").header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/supplies/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should let stock roles read a reservation and keep it from mechanics")
    void shouldLetStockRolesReadAReservationAndKeepItFromMechanics() throws Exception {
        String url = "/api/v1/stock-reservations/" + java.util.UUID.randomUUID();

        mockMvc.perform(get(url).header("Authorization", bearer("STOCKIST"))).andExpect(status().isNotFound());
        mockMvc.perform(get(url).header("Authorization", bearer("MECHANIC"))).andExpect(status().isForbidden());
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
    }
}
