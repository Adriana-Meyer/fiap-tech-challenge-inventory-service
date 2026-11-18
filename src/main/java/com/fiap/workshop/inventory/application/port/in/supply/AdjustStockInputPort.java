package com.fiap.workshop.inventory.application.port.in.supply;

import com.fiap.workshop.inventory.application.dto.supply.StockAdjustmentCommand;
import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;

import java.util.UUID;

public interface AdjustStockInputPort {
    SupplyResponse execute(UUID id, StockAdjustmentCommand command);
}
