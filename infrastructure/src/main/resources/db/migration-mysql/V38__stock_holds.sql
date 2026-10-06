-- Soft stock holds (cart / draft / sales order)

CREATE TABLE IF NOT EXISTS inv_stock_hold (
    id CHAR(36) NOT NULL PRIMARY KEY,
    company_id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    holder_id CHAR(36) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_id CHAR(36) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_inv_stock_hold_owner_product
    ON inv_stock_hold (company_id, owner_type, owner_id, product_id);

CREATE INDEX ix_inv_stock_hold_avail
    ON inv_stock_hold (company_id, warehouse_id, product_id, expires_at);

CREATE INDEX ix_inv_stock_hold_expires
    ON inv_stock_hold (expires_at);

CREATE INDEX ix_inv_stock_hold_owner
    ON inv_stock_hold (owner_type, owner_id);

CREATE TABLE IF NOT EXISTS inv_stock_hold_lock (
    company_id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    PRIMARY KEY (company_id, warehouse_id, product_id)
);
