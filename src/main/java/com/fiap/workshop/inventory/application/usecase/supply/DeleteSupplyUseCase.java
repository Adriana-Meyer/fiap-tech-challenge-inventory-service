package com.fiap.workshop.inventory.application.usecase.supply;

import com.fiap.workshop.inventory.application.port.in.supply.DeleteSupplyInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteSupplyUseCase implements DeleteSupplyInputPort {

    private final SupplyRepository supplyRepository;

    public DeleteSupplyUseCase(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    @Transactional
    public void execute(UUID id) {
        if (!supplyRepository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Supply", id);
        }
        supplyRepository.deleteById(id);
    }
}
