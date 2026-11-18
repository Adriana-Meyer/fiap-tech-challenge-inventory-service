package com.fiap.workshop.inventory.interfaces.rest;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.port.in.reservation.FindReservationInputPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-only: reservations are made, released and consumed by saga commands, never by HTTP. */
@RestController
@RequestMapping("/api/v1/stock-reservations")
public class StockReservationController {

    private final FindReservationInputPort findUseCase;

    public StockReservationController(FindReservationInputPort findUseCase) {
        this.findUseCase = findUseCase;
    }

    @GetMapping("/{serviceOrderId}")
    public ReservationResponse findByServiceOrderId(@PathVariable UUID serviceOrderId) {
        return findUseCase.findByServiceOrderId(serviceOrderId);
    }
}
