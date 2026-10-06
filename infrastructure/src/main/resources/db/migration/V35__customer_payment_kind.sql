-- Customer refunds against credit notes
ALTER TABLE acc_customer_payment ADD COLUMN IF NOT EXISTS payment_kind VARCHAR(16) NOT NULL DEFAULT 'PAYMENT';
