package com.fiap.workshop.inventory.application.port.in.supply;

import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;

import java.util.UUID;

public interface UpdateSupplyInputPort {
    SupplyResponse execute(UUID id, UpsertSupplyCommand command);
}
