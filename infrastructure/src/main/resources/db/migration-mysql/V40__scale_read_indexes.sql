-- Scale read paths: indexes for GL reports, partner filters, and date-bounded lists.
-- Additive only — no data rewrite.
-- Plain CREATE INDEX (MySQL rejects CREATE INDEX ... ALGORITHM=INPLACE, LOCK=NONE).

CREATE INDEX ix_je_company_status_date ON journal_entries (company_id, status, entry_date);

CREATE INDEX ix_je_company_partner_date ON journal_entries (company_id, partner_id, entry_date);

CREATE INDEX ix_ji_partner ON journal_items (partner_id);

CREATE INDEX ix_acc_ci_company_date ON acc_customer_invoice (company_id, invoice_date);

CREATE INDEX ix_acc_cp_company_date ON acc_customer_payment (company_id, payment_date);

CREATE INDEX ix_pur_vp_company_date ON pur_vendor_payment (company_id, payment_date);

CREATE INDEX ix_pur_vb_company_date ON pur_vendor_bill (company_id, bill_date);
