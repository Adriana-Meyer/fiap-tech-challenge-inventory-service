package com.fiap.workshop.inventory.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.workshop.inventory.application.dto.supply.StockAdjustmentCommand;
import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;
import com.fiap.workshop.inventory.application.port.in.supply.AdjustStockInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.CreateSupplyInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.DeleteSupplyInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.FindSupplyInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.UpdateSupplyInputPort;
import com.fiap.workshop.inventory.domain.exception.DuplicateResourceException;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import com.fiap.workshop.inventory.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplyController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateSupplyInputPort createUseCase;

    @MockBean
    private FindSupplyInputPort findUseCase;

    @MockBean
    private UpdateSupplyInputPort updateUseCase;

    @MockBean
    private DeleteSupplyInputPort deleteUseCase;

    @MockBean
    private AdjustStockInputPort adjustStockUseCase;

    @MockBean
    private JwtService jwtService;

    private final UUID id = UUID.randomUUID();

    private SupplyResponse sampleResponse() {
        return new SupplyResponse(id, "P001", "Oil Filter", "desc", "PART", BigDecimal.TEN, 10, 5, false);
    }

    @Test
    void shouldCreateSupply() throws Exception {
        UpsertSupplyCommand command = new UpsertSupplyCommand("P001", "Oil Filter", "desc", SupplyType.PART, BigDecimal.TEN, 5);
        when(createUseCase.execute(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/supplies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    void shouldFindAllSupplies() throws Exception {
        when(findUseCase.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/supplies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void shouldFindSuppliesBelowMinimum() throws Exception {
        when(findUseCase.findBelowMinimumStock()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/supplies").param("belowMinimum", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void shouldFindSupplyById() throws Exception {
        when(findUseCase.findById(id)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/supplies/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateSupply() throws Exception {
        UpsertSupplyCommand command = new UpsertSupplyCommand("P001", "Oil Filter", "desc", SupplyType.PART, BigDecimal.TEN, 5);
        when(updateUseCase.execute(eq(id), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/supplies/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteSupply() throws Exception {
        mockMvc.perform(delete("/api/v1/supplies/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldAdjustStock() throws Exception {
        StockAdjustmentCommand command = new StockAdjustmentCommand(5);
        when(adjustStockUseCase.execute(eq(id), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/supplies/{id}/stock", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAnswer404WhenSupplyIsNotFound() throws Exception {
        when(findUseCase.findById(id)).thenThrow(new ResourceNotFoundException("Supply", id));

        mockMvc.perform(get("/api/v1/supplies/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldAnswer409WhenCodeAlreadyExists() throws Exception {
        UpsertSupplyCommand command = new UpsertSupplyCommand("P001", "Oil Filter", "desc", SupplyType.PART, BigDecimal.TEN, 5);
        when(createUseCase.execute(any())).thenThrow(new DuplicateResourceException("Supply", "P001"));

        mockMvc.perform(post("/api/v1/supplies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAnswer400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/supplies")
                        .contentType("application/json")
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void shouldAnswer400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/supplies")
                        .contentType("application/json")
                        .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAnswer400WhenDomainRejectsTheArgument() throws Exception {
        when(adjustStockUseCase.execute(eq(id), any())).thenThrow(new IllegalArgumentException("Quantity must be positive"));

        mockMvc.perform(patch("/api/v1/supplies/{id}/stock", id)
                        .contentType("application/json")
                        .content("{\"quantity\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Quantity must be positive"));
    }

    @Test
    void shouldAnswer500WithoutLeakingDetailsOnUnexpectedErrors() throws Exception {
        when(findUseCase.findAll()).thenThrow(new IllegalStateException("db exploded"));

        mockMvc.perform(get("/api/v1/supplies"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
