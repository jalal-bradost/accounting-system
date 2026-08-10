-- Customer invoices can be normal invoices or credit notes (returns / AR credits).
ALTER TABLE acc_customer_invoice
    ADD COLUMN IF NOT EXISTS move_type VARCHAR(32) NOT NULL DEFAULT 'INVOICE',
    ADD COLUMN IF NOT EXISTS reversed_invoice_id UUID NULL;

CREATE INDEX IF NOT EXISTS ix_acc_ci_move_type ON acc_customer_invoice (company_id, move_type);
