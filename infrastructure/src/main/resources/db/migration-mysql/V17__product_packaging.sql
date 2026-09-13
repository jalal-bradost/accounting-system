-- Product packaging (qty in product base UOM) + document line snapshots.

CREATE TABLE IF NOT EXISTS inv_product_packaging (
    id CHAR(36) NOT NULL PRIMARY KEY,
    company_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    name VARCHAR(128) NOT NULL,
    qty DECIMAL(19, 4) NOT NULL,
    purchase_price DECIMAL(19, 4) NOT NULL DEFAULT 0,
    list_price DECIMAL(19, 4) NOT NULL DEFAULT 0,
    barcode VARCHAR(100) NULL,
    sku VARCHAR(64) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_base BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_inv_product_packaging_qty_positive CHECK (qty > 0)
);

UPDATE inv_product_packaging SET barcode = NULL WHERE barcode IS NOT NULL AND TRIM(barcode) = '';

CREATE UNIQUE INDEX ux_inv_product_packaging_name
    ON inv_product_packaging (company_id, product_id, name);

CREATE UNIQUE INDEX ux_inv_product_packaging_barcode
    ON inv_product_packaging (company_id, barcode);

CREATE INDEX ix_inv_product_packaging_product
    ON inv_product_packaging (product_id, active);

CREATE INDEX ix_inv_product_packaging_base
    ON inv_product_packaging (product_id, is_base);

-- Seed one base packaging per existing product.
INSERT INTO inv_product_packaging (
    id, company_id, product_id, name, qty, purchase_price, list_price, barcode, sku, active, is_base, created_at, updated_at
)
SELECT
    UUID(),
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
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM inv_product p
LEFT JOIN inv_uom u ON u.id = p.uom_id
WHERE NOT EXISTS (
    SELECT 1 FROM inv_product_packaging pk WHERE pk.product_id = p.id AND pk.is_base = TRUE
);

ALTER TABLE pur_purchase_order_line
    ADD COLUMN packaging_id CHAR(36) NULL,
    ADD COLUMN packaging_name VARCHAR(128) NULL,
    ADD COLUMN qty_per_package DECIMAL(19, 4) NULL;

ALTER TABLE sal_sales_order_line
    ADD COLUMN packaging_id CHAR(36) NULL,
    ADD COLUMN packaging_name VARCHAR(128) NULL,
    ADD COLUMN qty_per_package DECIMAL(19, 4) NULL;

ALTER TABLE pos_order_line
    ADD COLUMN packaging_id CHAR(36) NULL,
    ADD COLUMN packaging_name VARCHAR(128) NULL,
    ADD COLUMN qty_per_package DECIMAL(19, 4) NULL;
