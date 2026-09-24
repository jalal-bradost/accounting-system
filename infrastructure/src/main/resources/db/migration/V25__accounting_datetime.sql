ALTER TABLE acc_customer_payment
  ALTER COLUMN payment_date TYPE TIMESTAMP WITHOUT TIME ZONE
  USING payment_date::timestamp;
ALTER TABLE pur_vendor_payment
  ALTER COLUMN payment_date TYPE TIMESTAMP WITHOUT TIME ZONE
  USING payment_date::timestamp;
ALTER TABLE journal_entries
  ALTER COLUMN entry_date TYPE TIMESTAMP WITHOUT TIME ZONE
  USING entry_date::timestamp;
