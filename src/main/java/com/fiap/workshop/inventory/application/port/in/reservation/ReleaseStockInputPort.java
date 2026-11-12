package com.fiap.workshop.inventory.application.port.in.reservation;

import java.util.UUID;

public interface ReleaseStockInputPort {

    /** Gives the reserved stock back. Idempotent; a no-op when nothing was reserved. */
    void execute(UUID serviceOrderId);
}
