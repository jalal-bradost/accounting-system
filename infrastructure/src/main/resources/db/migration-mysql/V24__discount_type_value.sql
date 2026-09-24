-- Store the discount as entered (PERCENT or FIXED) so fixed amounts are never routed
-- through a rounded percentage. discount_percent is kept as a derived reporting column.

ALTER TABLE pur_purchase_order
    ADD COLUMN order_discount_type VARCHAR(16) NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN order_discount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
UPDATE pur_purchase_order SET order_discount_value = COALESCE(order_discount_percent, 0);

ALTER TABLE pur_purchase_order_line
    ADD COLUMN discount_type VARCHAR(16) NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN discount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
UPDATE pur_purchase_order_line SET discount_value = COALESCE(discount_percent, 0);

ALTER TABLE sal_sales_order
    ADD COLUMN order_discount_type VARCHAR(16) NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN order_discount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
UPDATE sal_sales_order SET order_discount_value = COALESCE(order_discount_percent, 0);

ALTER TABLE sal_sales_order_line
    ADD COLUMN discount_type VARCHAR(16) NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN discount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
UPDATE sal_sales_order_line SET discount_value = COALESCE(discount_percent, 0);

ALTER TABLE pur_vendor_bill_line
    ADD COLUMN discount_type VARCHAR(16) NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN discount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
UPDATE pur_vendor_bill_line SET discount_value = COALESCE(discount_percent, 0);

ALTER TABLE acc_customer_invoice_line
    ADD COLUMN discount_type VARCHAR(16) NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN discount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
UPDATE acc_customer_invoice_line SET discount_value = COALESCE(discount_percent, 0);
