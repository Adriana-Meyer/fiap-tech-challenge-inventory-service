package com.fiap.workshop.inventory.infrastructure.persistence.repository;

import com.fiap.workshop.inventory.infrastructure.persistence.entity.SupplyJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplyJpaRepository extends JpaRepository<SupplyJpaEntity, UUID> {

    Optional<SupplyJpaEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SupplyJpaEntity s WHERE s.id IN :ids ORDER BY s.id")
    List<SupplyJpaEntity> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

    @Query("SELECT s FROM SupplyJpaEntity s WHERE s.stockQuantity < s.minimumStock")
    List<SupplyJpaEntity> findBelowMinimumStock();
}
