package com.fiap.workshop.inventory.application.usecase.supply;

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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSupplyUseCase")
class FindSupplyUseCaseTest {

    @Mock
    private SupplyRepository supplyRepository;

    private FindSupplyUseCase useCase;
    private Supply supply;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new FindSupplyUseCase(supplyRepository);
        supply = new Supply(id, "P001", "Oil Filter", "desc", SupplyType.PART, Money.of(25.00), 10, 3);
    }

    @Test
    @DisplayName("should return supply response when found by id")
    void shouldReturnSupplyResponseWhenFoundById() {
        when(supplyRepository.findById(id)).thenReturn(Optional.of(supply));

        SupplyResponse response = useCase.findById(id);

        assertEquals(id, response.id());
        assertEquals("Oil Filter", response.name());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when supply not found by id")
    void shouldThrowResourceNotFoundExceptionWhenSupplyNotFoundById() {
        when(supplyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.findById(id));
    }

    @Test
    @DisplayName("should return all supplies as response list")
    void shouldReturnAllSuppliesAsResponseList() {
        when(supplyRepository.findAll()).thenReturn(List.of(supply));

        List<SupplyResponse> result = useCase.findAll();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).id());
    }

    @Test
    @DisplayName("should return supplies below minimum stock")
    void shouldReturnSuppliesBelowMinimumStock() {
        Supply belowMin = new Supply(UUID.randomUUID(), "P002", "Brake Pad", "desc", SupplyType.PART, Money.of(50.00), 1, 5);
        when(supplyRepository.findBelowMinimumStock()).thenReturn(List.of(belowMin));

        List<SupplyResponse> result = useCase.findBelowMinimumStock();

        assertEquals(1, result.size());
        assertTrue(result.get(0).belowMinimum());
    }
}
