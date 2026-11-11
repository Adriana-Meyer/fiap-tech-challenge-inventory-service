package com.fiap.workshop.inventory.domain.repository;

import com.fiap.workshop.inventory.domain.model.supply.Supply;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplyRepository {

    Supply save(Supply supply);

    Optional<Supply> findById(UUID id);

    Optional<Supply> findByCode(String code);

    List<Supply> findAll();

    List<Supply> findBelowMinimumStock();

    void deleteById(UUID id);

    boolean existsByCode(String code);
}
