package com.fiap.workshop.inventory.domain.repository;

import com.fiap.workshop.inventory.domain.model.supply.Supply;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplyRepository {

    Supply save(Supply supply);

    Optional<Supply> findById(UUID id);

    Optional<Supply> findByCode(String code);

    /**
     * Loads the supplies locking their rows until the transaction ends, in id
     * order so that two reservations touching the same supplies cannot
     * deadlock. Unknown ids are simply absent from the result.
     */
    List<Supply> findAllByIdForUpdate(java.util.Collection<UUID> ids);

    List<Supply> findAll();

    List<Supply> findBelowMinimumStock();

    void deleteById(UUID id);

    boolean existsByCode(String code);
}
