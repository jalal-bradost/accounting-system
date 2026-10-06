-- A payment may settle a document in another currency: `amount` stays in the DOCUMENT currency,
-- `payment_amount` is the part of the payment (in the PAYMENT currency) that it uses.
ALTER TABLE acc_customer_payment_allocation ADD COLUMN IF NOT EXISTS payment_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;
UPDATE acc_customer_payment_allocation SET payment_amount = amount;
ALTER TABLE pur_vendor_payment_allocation ADD COLUMN IF NOT EXISTS payment_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;
UPDATE pur_vendor_payment_allocation SET payment_amount = amount;
