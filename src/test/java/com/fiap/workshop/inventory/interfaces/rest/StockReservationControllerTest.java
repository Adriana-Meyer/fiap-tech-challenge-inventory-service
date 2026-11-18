package com.fiap.workshop.inventory.interfaces.rest;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.port.in.reservation.FindReservationInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindReservationInputPort findUseCase;

    @MockBean
    private JwtService jwtService;

    private final UUID orderId = UUID.randomUUID();

    @Test
    void shouldReturnTheReservationOfAnOrder() throws Exception {
        UUID supplyId = UUID.randomUUID();
        when(findUseCase.findByServiceOrderId(orderId)).thenReturn(new ReservationResponse(UUID.randomUUID(),
                orderId, "RESERVED", List.of(new ReservationResponse.Item(supplyId, 4)), Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/stock-reservations/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    @Test
    void shouldAnswer404WhenThereIsNoReservation() throws Exception {
        when(findUseCase.findByServiceOrderId(orderId)).thenThrow(new ResourceNotFoundException("StockReservation", orderId));

        mockMvc.perform(get("/api/v1/stock-reservations/{id}", orderId)).andExpect(status().isNotFound());
    }
}
