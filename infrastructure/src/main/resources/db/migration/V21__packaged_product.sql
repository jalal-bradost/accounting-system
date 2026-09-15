-- Link each non-base packaging definition to a system-managed packaged product
-- that holds pack-unit stock. Pack/Unpack moves qty between parent and this product.

ALTER TABLE inv_product ADD COLUMN IF NOT EXISTS parent_product_id UUID;
ALTER TABLE inv_product ADD COLUMN IF NOT EXISTS source_packaging_id UUID;

ALTER TABLE inv_product_packaging ADD COLUMN IF NOT EXISTS packaged_product_id UUID;

CREATE INDEX IF NOT EXISTS ix_inv_product_parent
    ON inv_product (parent_product_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_inv_product_source_packaging
    ON inv_product (source_packaging_id);
