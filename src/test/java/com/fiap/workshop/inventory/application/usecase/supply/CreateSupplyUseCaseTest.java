package com.fiap.workshop.inventory.application.usecase.supply;

import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;
import com.fiap.workshop.inventory.domain.exception.DuplicateResourceException;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.model.supply.SupplyType;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSupplyUseCase")
class CreateSupplyUseCaseTest {

    @Mock
    private SupplyRepository supplyRepository;

    private CreateSupplyUseCase useCase;
    private final UpsertSupplyCommand command = new UpsertSupplyCommand(
            "P001", "Oil Filter", "desc", SupplyType.PART, new BigDecimal("25.00"), 5);

    @BeforeEach
    void setUp() {
        useCase = new CreateSupplyUseCase(supplyRepository);
    }

    @Test
    @DisplayName("should save a new supply with zero stock")
    void shouldSaveANewSupplyWithZeroStock() {
        when(supplyRepository.existsByCode("P001")).thenReturn(false);
        when(supplyRepository.save(any(Supply.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplyResponse response = useCase.execute(command);

        ArgumentCaptor<Supply> saved = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepository).save(saved.capture());
        assertEquals(0, saved.getValue().getStockQuantity());
        assertEquals("P001", response.code());
        assertEquals(new BigDecimal("25.00"), response.unitPrice());
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when code already exists")
    void shouldThrowDuplicateResourceExceptionWhenCodeAlreadyExists() {
        when(supplyRepository.existsByCode("P001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> useCase.execute(command));
        verify(supplyRepository, never()).save(any());
    }
}
