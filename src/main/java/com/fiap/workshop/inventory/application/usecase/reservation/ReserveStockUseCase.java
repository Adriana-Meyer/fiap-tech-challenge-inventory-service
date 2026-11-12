package com.fiap.workshop.inventory.application.usecase.reservation;

import com.fiap.workshop.inventory.application.dto.reservation.ReservationResponse;
import com.fiap.workshop.inventory.application.dto.reservation.ReserveStockCommand;
import com.fiap.workshop.inventory.application.port.in.reservation.ReserveStockInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.reservation.ReservedItem;
import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.repository.StockReservationRepository;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReserveStockUseCase implements ReserveStockInputPort {

    private final StockReservationRepository reservationRepository;
    private final SupplyRepository supplyRepository;

    public ReserveStockUseCase(StockReservationRepository reservationRepository,
                               SupplyRepository supplyRepository) {
        this.reservationRepository = reservationRepository;
        this.supplyRepository = supplyRepository;
    }

    /** One transaction: if any supply falls short the exception rolls back every deduction made so far. */
    @Transactional
    public ReservationResponse execute(ReserveStockCommand command) {
        return reservationRepository.findByServiceOrderId(command.serviceOrderId())
                .map(ReservationResponse::from)
                .orElseGet(() -> reserve(command));
    }

    private ReservationResponse reserve(ReserveStockCommand command) {
        StockReservation reservation = StockReservation.reserve(command.serviceOrderId(),
                command.items().stream().map(line -> new ReservedItem(line.supplyId(), line.quantity())).toList());

        List<UUID> supplyIds = reservation.getItems().stream().map(ReservedItem::supplyId).toList();
        Map<UUID, Supply> supplies = supplyRepository.findAllByIdForUpdate(supplyIds).stream()
                .collect(Collectors.toMap(Supply::getId, Function.identity()));

        for (ReservedItem item : reservation.getItems()) {
            Supply supply = supplies.get(item.supplyId());
            if (supply == null) {
                throw new ResourceNotFoundException("Supply", item.supplyId());
            }
            supply.reserve(item.quantity());
            supplyRepository.save(supply);
        }
        return ReservationResponse.from(reservationRepository.save(reservation));
    }
}
