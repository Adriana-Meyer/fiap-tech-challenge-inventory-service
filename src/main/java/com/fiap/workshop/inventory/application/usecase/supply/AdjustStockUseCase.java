package com.fiap.workshop.inventory.application.usecase.supply;

import com.fiap.workshop.inventory.application.dto.supply.StockAdjustmentCommand;
import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.port.in.supply.AdjustStockInputPort;
import com.fiap.workshop.inventory.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.inventory.domain.model.supply.Supply;
import com.fiap.workshop.inventory.domain.repository.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdjustStockUseCase implements AdjustStockInputPort {

    private final SupplyRepository supplyRepository;

    public AdjustStockUseCase(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    @Transactional
    public SupplyResponse execute(UUID id, StockAdjustmentCommand command) {
        Supply supply = supplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supply", id));
        supply.addStock(command.quantity());
        return SupplyResponse.from(supplyRepository.save(supply));
    }
}
