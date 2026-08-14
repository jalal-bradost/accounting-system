-- Expense payment: real GL payment after post (bank/cash)

ALTER TABLE exp_expense ADD COLUMN IF NOT EXISTS payment_journal_entry_id UUID;
ALTER TABLE exp_expense ADD COLUMN IF NOT EXISTS payment_journal_id UUID;
ALTER TABLE exp_expense ADD COLUMN IF NOT EXISTS amount_paid DECIMAL(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE exp_expense ADD COLUMN IF NOT EXISTS payment_date DATE;
ALTER TABLE exp_expense ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255);
