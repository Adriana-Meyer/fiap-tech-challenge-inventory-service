package com.fiap.workshop.inventory.interfaces.rest;

import com.fiap.workshop.inventory.application.dto.supply.StockAdjustmentCommand;
import com.fiap.workshop.inventory.application.dto.supply.SupplyResponse;
import com.fiap.workshop.inventory.application.dto.supply.UpsertSupplyCommand;
import com.fiap.workshop.inventory.application.port.in.supply.AdjustStockInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.CreateSupplyInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.DeleteSupplyInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.FindSupplyInputPort;
import com.fiap.workshop.inventory.application.port.in.supply.UpdateSupplyInputPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplies")
public class SupplyController {

    private final CreateSupplyInputPort createUseCase;
    private final FindSupplyInputPort findUseCase;
    private final UpdateSupplyInputPort updateUseCase;
    private final DeleteSupplyInputPort deleteUseCase;
    private final AdjustStockInputPort adjustStockUseCase;

    public SupplyController(CreateSupplyInputPort createUseCase,
                             FindSupplyInputPort findUseCase,
                             UpdateSupplyInputPort updateUseCase,
                             DeleteSupplyInputPort deleteUseCase,
                             AdjustStockInputPort adjustStockUseCase) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.adjustStockUseCase = adjustStockUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplyResponse create(@Valid @RequestBody UpsertSupplyCommand command) {
        return createUseCase.execute(command);
    }

    @GetMapping
    public List<SupplyResponse> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean belowMinimum) {
        return belowMinimum ? findUseCase.findBelowMinimumStock() : findUseCase.findAll();
    }

    @GetMapping("/{id}")
    public SupplyResponse findById(@PathVariable UUID id) {
        return findUseCase.findById(id);
    }

    @PutMapping("/{id}")
    public SupplyResponse update(@PathVariable UUID id, @Valid @RequestBody UpsertSupplyCommand command) {
        return updateUseCase.execute(id, command);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/stock")
    public SupplyResponse adjustStock(@PathVariable UUID id,
                                       @Valid @RequestBody StockAdjustmentCommand command) {
        return adjustStockUseCase.execute(id, command);
    }
}
