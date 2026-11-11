package com.fiap.workshop.inventory.domain.model.supply;

import com.fiap.workshop.inventory.domain.model.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Supply")
class SupplyTest {

    private Supply supply() {
        return Supply.create("P001", "Oil Filter", "desc", SupplyType.PART, Money.of(25.00), 5);
    }

    @Test
    @DisplayName("should start with zero stock and a generated id")
    void shouldStartWithZeroStockAndGeneratedId() {
        Supply supply = supply();

        assertNotNull(supply.getId());
        assertEquals(0, supply.getStockQuantity());
        assertEquals(5, supply.getMinimumStock());
    }

    @Test
    @DisplayName("should add stock")
    void shouldAddStock() {
        Supply supply = supply();

        supply.addStock(7);

        assertEquals(7, supply.getStockQuantity());
    }

    @Test
    @DisplayName("should reject non positive quantity when adding stock")
    void shouldRejectNonPositiveQuantityWhenAddingStock() {
        Supply supply = supply();

        assertThrows(IllegalArgumentException.class, () -> supply.addStock(0));
        assertThrows(IllegalArgumentException.class, () -> supply.addStock(-1));
    }

    @Test
    @DisplayName("should be below minimum only while stock is under the minimum")
    void shouldBeBelowMinimumOnlyWhileStockIsUnderTheMinimum() {
        Supply supply = supply();
        assertTrue(supply.isBelowMinimum());

        supply.addStock(5);

        assertFalse(supply.isBelowMinimum());
    }

    @Test
    @DisplayName("should update descriptive data and keep code and stock")
    void shouldUpdateDescriptiveDataAndKeepCodeAndStock() {
        Supply supply = supply();
        supply.addStock(3);

        supply.update("Premium Filter", "new", SupplyType.CONSUMABLE, Money.of(30.00), 2);

        assertEquals("Premium Filter", supply.getName());
        assertEquals("new", supply.getDescription());
        assertEquals(SupplyType.CONSUMABLE, supply.getType());
        assertEquals(Money.of(30.00), supply.getUnitPrice());
        assertEquals(2, supply.getMinimumStock());
        assertEquals("P001", supply.getCode());
        assertEquals(3, supply.getStockQuantity());
    }
}
