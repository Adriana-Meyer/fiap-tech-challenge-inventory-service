package com.fiap.workshop.inventory.application.usecase.supply;

import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;
import com.fiap.workshop.inventory.application.port.in.supply.UpdateSupplyInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.shared.Money;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateSupplyUseCase implements UpdateSupplyInputPort {

    private final SupplyRepository supplyRepository;

    public UpdateSupplyUseCase(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    @Transactional
    public SupplyResponse execute(UUID id, UpsertSupplyCommand command) {
        Supply supply = supplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supply", id));
        supply.update(command.name(), command.description(), command.type(),
                Money.of(command.unitPrice()), command.minimumStock());
        return SupplyResponse.from(supplyRepository.save(supply));
    }
}
