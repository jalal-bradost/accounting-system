-- Company-level document policies: bill without receipt / invoice without delivery

ALTER TABLE platform_company
    ADD COLUMN IF NOT EXISTS allow_bill_without_receipt BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE platform_company
    ADD COLUMN IF NOT EXISTS allow_invoice_without_delivery BOOLEAN NOT NULL DEFAULT FALSE;
