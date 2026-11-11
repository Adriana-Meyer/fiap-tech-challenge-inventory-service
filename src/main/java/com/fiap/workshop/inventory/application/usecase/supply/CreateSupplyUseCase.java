package com.fiap.workshop.inventory.application.usecase.supply;

import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;
import com.fiap.workshop.inventory.application.port.in.supply.CreateSupplyInputPort;
import com.fiap.workshop.inventory.domain.exception.DuplicateResourceException;
import com.fiap.workshop.inventory.domain.model.shared.Money;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSupplyUseCase implements CreateSupplyInputPort {

    private final SupplyRepository supplyRepository;

    public CreateSupplyUseCase(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    @Transactional
    public SupplyResponse execute(UpsertSupplyCommand command) {
        if (supplyRepository.existsByCode(command.code())) {
            throw new DuplicateResourceException("Supply", command.code());
        }
        Supply supply = Supply.create(
                command.code(),
                command.name(),
                command.description(),
                command.type(),
                Money.of(command.unitPrice()),
                command.minimumStock()
        );
        return SupplyResponse.from(supplyRepository.save(supply));
    }
}
