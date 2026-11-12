package com.fiap.workshop.inventory.infrastructure.persistence.adapter;

import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import com.fiap.workshop.inventory.infrastructure.persistence.mapper.SupplyMapper;
import com.fiap.workshop.inventory.infrastructure.persistence.repository.SupplyJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SupplyRepositoryAdapter implements SupplyRepository {

    private final SupplyJpaRepository jpaRepository;
    private final SupplyMapper mapper;

    public SupplyRepositoryAdapter(SupplyJpaRepository jpaRepository, SupplyMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Supply save(Supply supply) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(supply)));
    }

    @Override
    public Optional<Supply> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Supply> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Supply> findAllByIdForUpdate(java.util.Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByIdForUpdate(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Supply> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Supply> findBelowMinimumStock() {
        return jpaRepository.findBelowMinimumStock().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }
}
