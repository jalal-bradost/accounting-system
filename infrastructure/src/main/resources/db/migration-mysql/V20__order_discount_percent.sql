ALTER TABLE pur_purchase_order ADD COLUMN order_discount_percent DECIMAL(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE sal_sales_order ADD COLUMN order_discount_percent DECIMAL(19, 4) NOT NULL DEFAULT 0;
