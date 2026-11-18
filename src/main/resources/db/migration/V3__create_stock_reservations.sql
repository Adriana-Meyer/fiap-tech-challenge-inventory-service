-- Stock held for a service order: one reservation per order, with its lines.

CREATE TABLE stock_reservations (
    id               UUID        NOT NULL,
    service_order_id UUID        NOT NULL,
    status           VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_stock_reservations PRIMARY KEY (id),
    CONSTRAINT uq_stock_reservations_service_order UNIQUE (service_order_id)
);

CREATE TABLE stock_reservation_items (
    id             UUID NOT NULL,
    reservation_id UUID NOT NULL,
    position       INT  NOT NULL,
    supply_id      UUID NOT NULL,
    quantity       INT  NOT NULL,
    CONSTRAINT pk_stock_reservation_items PRIMARY KEY (id),
    CONSTRAINT fk_reservation_items_reservation FOREIGN KEY (reservation_id)
        REFERENCES stock_reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_items_supply FOREIGN KEY (supply_id) REFERENCES supplies (id),
    CONSTRAINT chk_reservation_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_reservation_items_reservation ON stock_reservation_items (reservation_id);
