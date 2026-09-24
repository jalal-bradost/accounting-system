-- Purchase corrections: vendor payment kind, PO lock, return to_refund

ALTER TABLE pur_vendor_payment
    ADD COLUMN payment_kind VARCHAR(16) NOT NULL DEFAULT 'PAYOUT';

ALTER TABLE pur_purchase_order
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE inv_stock_picking
    ADD COLUMN to_refund BOOLEAN NOT NULL DEFAULT TRUE;
