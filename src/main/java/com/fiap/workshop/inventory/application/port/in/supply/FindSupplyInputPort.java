package com.fiap.workshop.inventory.application.port.in.supply;

import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;

import java.util.List;
import java.util.UUID;

public interface FindSupplyInputPort {
    SupplyResponse findById(UUID id);
    List<SupplyResponse> findAll();
    List<SupplyResponse> findBelowMinimumStock();
}
