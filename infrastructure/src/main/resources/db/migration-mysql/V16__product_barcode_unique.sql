-- Unique barcode per company. MySQL allows multiple NULLs in UNIQUE indexes.
-- Blank barcodes must be stored as NULL by the application.
UPDATE inv_product SET barcode = NULL WHERE barcode IS NOT NULL AND TRIM(barcode) = '';

CREATE UNIQUE INDEX ux_inv_product_company_barcode
    ON inv_product (company_id, barcode);

CREATE INDEX ix_inv_product_company_barcode
    ON inv_product (company_id, barcode);
