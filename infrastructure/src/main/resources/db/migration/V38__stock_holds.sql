-- Soft stock holds (cart / draft / sales order)

CREATE TABLE IF NOT EXISTS inv_stock_hold (
    id UUID NOT NULL PRIMARY KEY,
    company_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    holder_id UUID NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_inv_stock_hold_owner_product
    ON inv_stock_hold (company_id, owner_type, owner_id, product_id);

CREATE INDEX IF NOT EXISTS ix_inv_stock_hold_avail
    ON inv_stock_hold (company_id, warehouse_id, product_id, expires_at);

CREATE INDEX IF NOT EXISTS ix_inv_stock_hold_expires
    ON inv_stock_hold (expires_at);

CREATE INDEX IF NOT EXISTS ix_inv_stock_hold_owner
    ON inv_stock_hold (owner_type, owner_id);

-- Row-level lock keys so concurrent claims serialize even when no quant exists yet
CREATE TABLE IF NOT EXISTS inv_stock_hold_lock (
    company_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    PRIMARY KEY (company_id, warehouse_id, product_id)
);
