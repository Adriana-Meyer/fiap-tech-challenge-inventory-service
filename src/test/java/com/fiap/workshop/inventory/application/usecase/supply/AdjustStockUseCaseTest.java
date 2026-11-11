package com.fiap.workshop.inventory.application.usecase.supply;

import com.fiap.workshop.inventory.application.dto.supply.StockAdjustmentCommand;
import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.shared.Money;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdjustStockUseCase")
class AdjustStockUseCaseTest {

    @Mock
    private SupplyRepository supplyRepository;

    private AdjustStockUseCase useCase;
    private Supply supply;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new AdjustStockUseCase(supplyRepository);
        supply = new Supply(id, "P001", "Oil Filter", "desc", SupplyType.PART, Money.of(25.00), 5, 3);
    }

    @Test
    @DisplayName("should add stock and return updated response")
    void shouldAddStockAndReturnUpdatedResponse() {
        when(supplyRepository.findById(id)).thenReturn(Optional.of(supply));
        when(supplyRepository.save(supply)).thenReturn(supply);

        SupplyResponse response = useCase.execute(id, new StockAdjustmentCommand(10));

        assertEquals(15, supply.getStockQuantity());
        verify(supplyRepository).save(supply);
        assertNotNull(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when supply not found")
    void shouldThrowResourceNotFoundExceptionWhenSupplyNotFound() {
        when(supplyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(id, new StockAdjustmentCommand(10)));
        verify(supplyRepository, never()).save(any());
    }
}
