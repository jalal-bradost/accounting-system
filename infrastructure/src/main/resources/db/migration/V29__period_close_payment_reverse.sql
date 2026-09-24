-- Irreversible fiscal close metadata
ALTER TABLE fiscal_periods ADD COLUMN closed_at TIMESTAMP;
ALTER TABLE fiscal_periods ADD COLUMN closed_by UUID;

-- Customer payment state + reversal link
ALTER TABLE acc_customer_payment ADD COLUMN state VARCHAR(20) NOT NULL DEFAULT 'POSTED';
ALTER TABLE acc_customer_payment ADD COLUMN reversal_journal_entry_id UUID;

CREATE INDEX ix_acc_cp_company_state ON acc_customer_payment(company_id, state);

-- Vendor payment reversal link
ALTER TABLE pur_vendor_payment ADD COLUMN reversal_journal_entry_id UUID;
