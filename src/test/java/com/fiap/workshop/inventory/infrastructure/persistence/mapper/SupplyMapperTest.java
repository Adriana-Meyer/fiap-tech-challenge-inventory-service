package com.fiap.workshop.inventory.infrastructure.persistence.mapper;

import com.fiap.workshop.inventory.domain.model.shared.Money;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import com.fiap.workshop.inventory.infrastructure.persistence.entity.SupplyJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SupplyMapper")
class SupplyMapperTest {

    private final SupplyMapper mapper = new SupplyMapper();

    @Test
    @DisplayName("should round trip a supply through the JPA entity")
    void shouldRoundTripASupplyThroughTheJpaEntity() {
        UUID id = UUID.randomUUID();
        Supply supply = new Supply(id, "P001", "Oil Filter", "desc", SupplyType.PART, Money.of(25.00), 10, 3);

        SupplyJpaEntity entity = mapper.toEntity(supply);
        Supply restored = mapper.toDomain(entity);

        assertEquals(id, entity.getId());
        assertEquals("PART", entity.getType());
        assertEquals(new BigDecimal("25.00"), entity.getUnitPrice());
        assertEquals(id, restored.getId());
        assertEquals("P001", restored.getCode());
        assertEquals(SupplyType.PART, restored.getType());
        assertEquals(Money.of(25.00), restored.getUnitPrice());
        assertEquals(10, restored.getStockQuantity());
        assertEquals(3, restored.getMinimumStock());
    }
}
