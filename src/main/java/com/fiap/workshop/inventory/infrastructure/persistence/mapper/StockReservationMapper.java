package com.fiap.workshop.inventory.infrastructure.persistence.mapper;

import com.fiap.workshop.inventory.domain.model.reservation.ReservationStatus;
import com.fiap.workshop.inventory.domain.model.reservation.ReservedItem;
import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.infrastructure.persistence.entity.StockReservationItemJpaEntity;
import com.fiap.workshop.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
public class StockReservationMapper {

    public StockReservation toDomain(StockReservationJpaEntity e) {
        List<ReservedItem> items = e.getItems().stream()
                .map(i -> new ReservedItem(i.getSupplyId(), i.getQuantity())).toList();
        return new StockReservation(e.getId(), e.getServiceOrderId(), items,
                ReservationStatus.valueOf(e.getStatus()), e.getCreatedAt(), e.getUpdatedAt());
    }

    public StockReservationJpaEntity toEntity(StockReservation r) {
        List<ReservedItem> items = r.getItems();
        List<StockReservationItemJpaEntity> itemEntities = IntStream.range(0, items.size())
                .mapToObj(i -> new StockReservationItemJpaEntity(UUID.randomUUID(), i,
                        items.get(i).supplyId(), items.get(i).quantity()))
                .toList();
        return new StockReservationJpaEntity(r.getId(), r.getServiceOrderId(), r.getStatus().name(),
                r.getCreatedAt(), r.getUpdatedAt(), itemEntities);
    }
}
