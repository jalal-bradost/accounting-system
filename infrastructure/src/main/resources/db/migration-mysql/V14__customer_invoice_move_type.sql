-- Customer invoices can be normal invoices or credit notes (returns / AR credits).
ALTER TABLE acc_customer_invoice ADD COLUMN move_type VARCHAR(32) NOT NULL DEFAULT 'INVOICE';
ALTER TABLE acc_customer_invoice ADD COLUMN reversed_invoice_id CHAR(36) NULL;

CREATE INDEX ix_acc_ci_move_type ON acc_customer_invoice (company_id, move_type);