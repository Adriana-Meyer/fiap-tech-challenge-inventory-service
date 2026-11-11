package com.fiap.workshop.inventory.infrastructure.persistence.adapter;

import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import com.fiap.workshop.inventory.infrastructure.persistence.entity.SupplyJpaEntity;
import com.fiap.workshop.inventory.infrastructure.persistence.mapper.SupplyMapper;
import com.fiap.workshop.inventory.infrastructure.persistence.repository.SupplyJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplyRepositoryAdapterTest {

    @Mock
    private SupplyJpaRepository jpaRepository;

    @Mock
    private SupplyMapper mapper;

    private SupplyRepositoryAdapter adapter;

    private final UUID id = UUID.randomUUID();
    private Supply supply;
    private SupplyJpaEntity entity;

    @BeforeEach
    void setUp() {
        adapter = new SupplyRepositoryAdapter(jpaRepository, mapper);
        supply = Supply.create("P001", "Oil Filter", "desc", SupplyType.PART, com.fiap.workshop.inventory.domain.model.shared.Money.of(BigDecimal.TEN), 5);
        entity = new SupplyJpaEntity(id, "P001", "Oil Filter", "desc", "PART", BigDecimal.TEN, 10, 5);
    }

    @Test
    void shouldSaveAndReturnDomainSupply() {
        when(mapper.toEntity(supply)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(supply);

        Supply result = adapter.save(supply);

        assertEquals(supply, result);
    }

    @Test
    void shouldFindById() {
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(supply);

        assertTrue(adapter.findById(id).isPresent());
    }

    @Test
    void shouldFindByCode() {
        when(jpaRepository.findByCode("P001")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(supply);

        assertTrue(adapter.findByCode("P001").isPresent());
    }

    @Test
    void shouldFindAll() {
        when(jpaRepository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(supply);

        assertEquals(1, adapter.findAll().size());
    }

    @Test
    void shouldFindBelowMinimumStock() {
        when(jpaRepository.findBelowMinimumStock()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(supply);

        assertEquals(1, adapter.findBelowMinimumStock().size());
    }

    @Test
    void shouldDeleteById() {
        adapter.deleteById(id);

        verify(jpaRepository).deleteById(id);
    }

    @Test
    void shouldCheckExistsByCode() {
        when(jpaRepository.existsByCode("P001")).thenReturn(true);

        assertTrue(adapter.existsByCode("P001"));
    }
}
