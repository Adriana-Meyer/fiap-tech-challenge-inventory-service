package com.fiap.workshop.inventory.application.usecase.reservation;

import com.fiap.workshop.inventory.application.port.in.reservation.ReleaseStockInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.reservation.ReservationStatus;
import com.fiap.workshop.inventory.domain.model.reservation.ReservedItem;
import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.repository.StockReservationRepository;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReleaseStockUseCase implements ReleaseStockInputPort {

    private final StockReservationRepository reservationRepository;
    private final SupplyRepository supplyRepository;

    public ReleaseStockUseCase(StockReservationRepository reservationRepository,
                               SupplyRepository supplyRepository) {
        this.reservationRepository = reservationRepository;
        this.supplyRepository = supplyRepository;
    }

    @Transactional
    public void execute(UUID serviceOrderId) {
        Optional<StockReservation> found = reservationRepository.findByServiceOrderId(serviceOrderId);
        if (found.isEmpty() || found.get().getStatus() == ReservationStatus.RELEASED) {
            return;
        }
        StockReservation reservation = found.get();
        reservation.release();

        Map<UUID, Supply> supplies = supplyRepository.findAllByIdForUpdate(
                        reservation.getItems().stream().map(ReservedItem::supplyId).toList()).stream()
                .collect(Collectors.toMap(Supply::getId, Function.identity()));
        for (ReservedItem item : reservation.getItems()) {
            Supply supply = supplies.get(item.supplyId());
            if (supply == null) {
                throw new ResourceNotFoundException("Supply", item.supplyId());
            }
            supply.addStock(item.quantity());
            supplyRepository.save(supply);
        }
        reservationRepository.save(reservation);
    }
}
