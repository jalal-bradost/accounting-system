ALTER TABLE pur_purchase_order
    ADD COLUMN IF NOT EXISTS order_discount_percent NUMERIC(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE sal_sales_order
    ADD COLUMN IF NOT EXISTS order_discount_percent NUMERIC(19, 4) NOT NULL DEFAULT 0;
