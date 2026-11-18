-- Inventory service: owns the supplies (parts and consumables) and their stock.

CREATE TABLE supplies (
    id             UUID          NOT NULL,
    code           VARCHAR(50)   NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    description    TEXT,
    type           VARCHAR(20)   NOT NULL,
    unit_price     NUMERIC(15,2) NOT NULL,
    stock_quantity INT           NOT NULL DEFAULT 0,
    minimum_stock  INT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_supplies PRIMARY KEY (id),
    CONSTRAINT uq_supplies_code UNIQUE (code)
);
