package com.fiap.workshop.inventory.infrastructure.persistence.repository;

import com.fiap.workshop.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationJpaEntity, UUID> {

    Optional<StockReservationJpaEntity> findByServiceOrderId(UUID serviceOrderId);
}
