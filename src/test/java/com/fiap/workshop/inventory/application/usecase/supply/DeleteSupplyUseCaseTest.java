package com.fiap.workshop.inventory.application.usecase.supply;

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
@DisplayName("DeleteSupplyUseCase")
class DeleteSupplyUseCaseTest {

    @Mock
    private SupplyRepository supplyRepository;

    private DeleteSupplyUseCase useCase;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new DeleteSupplyUseCase(supplyRepository);
    }

    @Test
    @DisplayName("should delete supply when found")
    void shouldDeleteSupplyWhenFound() {
        Supply supply = new Supply(id, "P001", "Oil Filter", "desc", SupplyType.PART, Money.of(25.00), 10, 3);
        when(supplyRepository.findById(id)).thenReturn(Optional.of(supply));

        assertDoesNotThrow(() -> useCase.execute(id));

        verify(supplyRepository).deleteById(id);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when supply not found")
    void shouldThrowResourceNotFoundExceptionWhenSupplyNotFound() {
        when(supplyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
        verify(supplyRepository, never()).deleteById(any());
    }
}
