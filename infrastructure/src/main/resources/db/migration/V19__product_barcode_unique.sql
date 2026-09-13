-- Unique barcode per company. Empty barcodes stored as NULL so multiple blanks are allowed.
-- (Partial indexes are Postgres-only; this form works on H2 MODE=PostgreSQL and Postgres.)
UPDATE inv_product SET barcode = NULL WHERE barcode IS NOT NULL AND TRIM(barcode) = '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_inv_product_company_barcode
    ON inv_product (company_id, barcode);

CREATE INDEX IF NOT EXISTS ix_inv_product_company_barcode
    ON inv_product (company_id, barcode);
