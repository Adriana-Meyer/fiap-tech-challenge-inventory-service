package com.fiap.workshop.inventory.application.port.in.supply;

import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;

public interface CreateSupplyInputPort {
    SupplyResponse execute(UpsertSupplyCommand command);
}
