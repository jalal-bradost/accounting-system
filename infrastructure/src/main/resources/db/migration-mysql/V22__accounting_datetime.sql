ALTER TABLE acc_customer_payment MODIFY payment_date DATETIME(6) NOT NULL;
ALTER TABLE pur_vendor_payment MODIFY payment_date DATETIME(6) NOT NULL;
ALTER TABLE journal_entries MODIFY entry_date DATETIME(6) NOT NULL;
