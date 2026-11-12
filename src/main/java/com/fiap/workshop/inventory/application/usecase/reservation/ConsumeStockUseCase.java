package com.fiap.workshop.inventory.application.usecase.reservation;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.port.in.reservation.ConsumeStockInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.reservation.ReservationStatus;
import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.domain.repository.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** The stock left the shelf when it was reserved, so consuming only closes the reservation. */
@Service
public class ConsumeStockUseCase implements ConsumeStockInputPort {

    private final StockReservationRepository reservationRepository;

    public ConsumeStockUseCase(StockReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ReservationResponse execute(UUID serviceOrderId) {
        StockReservation reservation = reservationRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("StockReservation", serviceOrderId));
        if (reservation.getStatus() == ReservationStatus.CONSUMED) {
            return ReservationResponse.from(reservation);
        }
        reservation.consume();
        return ReservationResponse.from(reservationRepository.save(reservation));
    }
}
