package com.fiap.workshop.inventory.application.usecase.supply;

import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateSupplyUseCase")
class UpdateSupplyUseCaseTest {

    @Mock
    private SupplyRepository supplyRepository;

    private UpdateSupplyUseCase useCase;
    private Supply supply;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new UpdateSupplyUseCase(supplyRepository);
        supply = new Supply(id, "P001", "Oil Filter", "old desc", SupplyType.PART, Money.of(25.00), 10, 3);
    }

    @Test
    @DisplayName("should update supply and return updated response")
    void shouldUpdateSupplyAndReturnUpdatedResponse() {
        UpsertSupplyCommand command = new UpsertSupplyCommand("P001", "Premium Filter", "new desc",
                SupplyType.PART, new BigDecimal("30.00"), 5);
        when(supplyRepository.findById(id)).thenReturn(Optional.of(supply));
        when(supplyRepository.save(supply)).thenReturn(supply);

        SupplyResponse response = useCase.execute(id, command);

        assertEquals("Premium Filter", supply.getName());
        assertEquals(5, supply.getMinimumStock());
        verify(supplyRepository).save(supply);
        assertNotNull(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when supply not found")
    void shouldThrowResourceNotFoundExceptionWhenSupplyNotFound() {
        UpsertSupplyCommand command = new UpsertSupplyCommand("P001", "Premium Filter", "desc",
                SupplyType.PART, new BigDecimal("30.00"), 5);
        when(supplyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id, command));
        verify(supplyRepository, never()).save(any());
    }
}
