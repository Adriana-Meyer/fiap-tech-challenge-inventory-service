package com.fiap.workshop.inventory.infrastructure.persistence.repository;

import com.fiap.workshop.inventory.infrastructure.persistence.entity.SupplyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplyJpaRepository extends JpaRepository<SupplyJpaEntity, UUID> {

    Optional<SupplyJpaEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT s FROM SupplyJpaEntity s WHERE s.stockQuantity < s.minimumStock")
    List<SupplyJpaEntity> findBelowMinimumStock();
}
