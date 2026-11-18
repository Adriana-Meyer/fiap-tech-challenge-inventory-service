package com.fiap.workshop.inventory.application.port.in.supply;

import java.util.UUID;

public interface DeleteSupplyInputPort {
    void execute(UUID id);
}
