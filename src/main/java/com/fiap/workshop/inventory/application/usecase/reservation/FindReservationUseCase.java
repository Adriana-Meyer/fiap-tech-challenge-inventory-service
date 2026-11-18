package com.fiap.workshop.inventory.application.usecase.reservation;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.port.in.reservation.FindReservationInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.repository.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindReservationUseCase implements FindReservationInputPort {

    private final StockReservationRepository reservationRepository;

    public FindReservationUseCase(StockReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public ReservationResponse findByServiceOrderId(UUID serviceOrderId) {
        return reservationRepository.findByServiceOrderId(serviceOrderId)
                .map(ReservationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("StockReservation", serviceOrderId));
    }
}
