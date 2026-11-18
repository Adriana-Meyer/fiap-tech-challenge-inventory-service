package com.fiap.workshop.inventory.infrastructure.persistence.adapter;

import com.fiap.workshop.inventory.domain.model.reservation.StockReservation;
import com.fiap.workshop.inventory.domain.repository.StockReservationRepository;
import com.fiap.workshop.inventory.infrastructure.persistence.mapper.StockReservationMapper;
import com.fiap.workshop.inventory.infrastructure.persistence.repository.StockReservationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StockReservationRepositoryAdapter implements StockReservationRepository {

    private final StockReservationJpaRepository jpaRepository;
    private final StockReservationMapper mapper;

    public StockReservationRepositoryAdapter(StockReservationJpaRepository jpaRepository,
                                             StockReservationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StockReservation save(StockReservation reservation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(reservation)));
    }

    @Override
    public Optional<StockReservation> findByServiceOrderId(UUID serviceOrderId) {
        return jpaRepository.findByServiceOrderId(serviceOrderId).map(mapper::toDomain);
    }
}
