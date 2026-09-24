ALTER TABLE inv_product
    ADD COLUMN parent_product_id CHAR(36) NULL,
    ADD COLUMN source_packaging_id CHAR(36) NULL;

ALTER TABLE inv_product_packaging
    ADD COLUMN packaged_product_id CHAR(36) NULL;

CREATE INDEX ix_inv_product_parent ON inv_product (parent_product_id);
CREATE UNIQUE INDEX ux_inv_product_source_packaging ON inv_product (source_packaging_id);
