-- Keep order-level discounts on bills/invoices instead of folding them into product lines.
ALTER TABLE pur_vendor_bill
    ADD COLUMN order_discount_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;

ALTER TABLE acc_customer_invoice
    ADD COLUMN order_discount_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;
