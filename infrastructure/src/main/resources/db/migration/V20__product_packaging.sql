-- Product packaging (qty in product base UOM) + document line snapshots.
-- Indexes avoid Postgres-only partial/expression forms so H2 (dev) can migrate.

CREATE TABLE IF NOT EXISTS inv_product_packaging (
    id UUID NOT NULL PRIMARY KEY,
    company_id UUID NOT NULL,
    product_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    qty NUMERIC(19, 4) NOT NULL,
    purchase_price NUMERIC(19, 4) NOT NULL DEFAULT 0,
    list_price NUMERIC(19, 4) NOT NULL DEFAULT 0,
    barcode VARCHAR(100),
    sku VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_base BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inv_product_packaging_qty_positive CHECK (qty > 0)
);

UPDATE inv_product_packaging SET barcode = NULL WHERE barcode IS NOT NULL AND TRIM(barcode) = '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_inv_product_packaging_name
    ON inv_product_packaging (company_id, product_id, name);

CREATE UNIQUE INDEX IF NOT EXISTS ux_inv_product_packaging_barcode
    ON inv_product_packaging (company_id, barcode);

CREATE INDEX IF NOT EXISTS ix_inv_product_packaging_product
    ON inv_product_packaging (product_id, active);

CREATE INDEX IF NOT EXISTS ix_inv_product_packaging_base
    ON inv_product_packaging (product_id, is_base);

-- Seed one base packaging per existing product (qty=1, prices from product).
INSERT INTO inv_product_packaging (
    id, company_id, product_id, name, qty, purchase_price, list_price, barcode, sku, active, is_base, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    p.company_id,
    p.id,
    COALESCE(NULLIF(TRIM(u.name), ''), 'Unit'),
    1,
    COALESCE(p.standard_cost, 0),
    COALESCE(p.list_price, 0),
    NULLIF(TRIM(p.barcode), ''),
    NULL,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM inv_product p
LEFT JOIN inv_uom u ON u.id = p.uom_id
WHERE NOT EXISTS (
    SELECT 1 FROM inv_product_packaging pk WHERE pk.product_id = p.id AND pk.is_base = TRUE
);

ALTER TABLE pur_purchase_order_line ADD COLUMN IF NOT EXISTS packaging_id UUID;
ALTER TABLE pur_purchase_order_line ADD COLUMN IF NOT EXISTS packaging_name VARCHAR(128);
ALTER TABLE pur_purchase_order_line ADD COLUMN IF NOT EXISTS qty_per_package NUMERIC(19, 4);

ALTER TABLE sal_sales_order_line ADD COLUMN IF NOT EXISTS packaging_id UUID;
ALTER TABLE sal_sales_order_line ADD COLUMN IF NOT EXISTS packaging_name VARCHAR(128);
ALTER TABLE sal_sales_order_line ADD COLUMN IF NOT EXISTS qty_per_package NUMERIC(19, 4);

ALTER TABLE pos_order_line ADD COLUMN IF NOT EXISTS packaging_id UUID;
ALTER TABLE pos_order_line ADD COLUMN IF NOT EXISTS packaging_name VARCHAR(128);
ALTER TABLE pos_order_line ADD COLUMN IF NOT EXISTS qty_per_package NUMERIC(19, 4);
