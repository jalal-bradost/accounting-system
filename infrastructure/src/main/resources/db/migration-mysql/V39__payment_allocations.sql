-- Payment allocations: payments are no longer tied 1:1 to a document. Allocations are the only
-- source of truth for paid / outstanding / unallocated amounts.

CREATE TABLE acc_customer_payment_allocation (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    payment_id CHAR(36) NOT NULL,
    customer_invoice_id CHAR(36) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    amount_company DECIMAL(19, 4) NOT NULL,
    payment_amount_company DECIMAL(19, 4) NOT NULL,
    allocation_date DATE NOT NULL,
    fx_journal_entry_id CHAR(36) NULL,
    fx_reversal_journal_entry_id CHAR(36) NULL,
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_acc_cpa_payment ON acc_customer_payment_allocation(payment_id);
CREATE INDEX ix_acc_cpa_invoice_state ON acc_customer_payment_allocation(customer_invoice_id, state);
CREATE INDEX ix_acc_cpa_company ON acc_customer_payment_allocation(company_id);

CREATE TABLE pur_vendor_payment_allocation (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    payment_id CHAR(36) NOT NULL,
    vendor_bill_id CHAR(36) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    amount_company DECIMAL(19, 4) NOT NULL,
    payment_amount_company DECIMAL(19, 4) NOT NULL,
    allocation_date DATE NOT NULL,
    fx_journal_entry_id CHAR(36) NULL,
    fx_reversal_journal_entry_id CHAR(36) NULL,
    state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_pur_vpa_payment ON pur_vendor_payment_allocation(payment_id);
CREATE INDEX ix_pur_vpa_bill_state ON pur_vendor_payment_allocation(vendor_bill_id, state);
CREATE INDEX ix_pur_vpa_company ON pur_vendor_payment_allocation(company_id);

-- Backfill: one allocation per legacy payment (allocation id = payment id). The company amounts are
-- the receivable/payable amount actually booked on the payment entry (legacy entries embedded FX at
-- the document rate, so document side and payment side are equal and no FX entry is needed).
-- Cross-currency legacy payments were never counted in outstanding; they stay unallocated.
INSERT INTO acc_customer_payment_allocation (
    id, company_id, payment_id, customer_invoice_id, amount, amount_company, payment_amount_company,
    allocation_date, fx_journal_entry_id, fx_reversal_journal_entry_id, state, created_at, updated_at)
SELECT p.id, p.company_id, p.id, p.customer_invoice_id, p.amount,
       COALESCE((SELECT ABS(SUM(ji.debit - ji.credit)) FROM journal_items ji
                 JOIN accounts a ON a.id = ji.account_id
                 WHERE ji.journal_entry_id = p.journal_entry_id AND a.type = 'RECEIVABLE'),
                ROUND(p.amount * p.exchange_rate_to_company, 4)),
       COALESCE((SELECT ABS(SUM(ji.debit - ji.credit)) FROM journal_items ji
                 JOIN accounts a ON a.id = ji.account_id
                 WHERE ji.journal_entry_id = p.journal_entry_id AND a.type = 'RECEIVABLE'),
                ROUND(p.amount * p.exchange_rate_to_company, 4)),
       CAST(p.payment_date AS DATE), NULL, NULL,
       CASE WHEN p.state = 'POSTED' THEN 'ACTIVE' ELSE 'REVERSED' END,
       p.created_at, p.updated_at
FROM acc_customer_payment p
JOIN acc_customer_invoice i ON i.id = p.customer_invoice_id
WHERE UPPER(i.currency_code) = UPPER(p.currency_code);

INSERT INTO pur_vendor_payment_allocation (
    id, company_id, payment_id, vendor_bill_id, amount, amount_company, payment_amount_company,
    allocation_date, fx_journal_entry_id, fx_reversal_journal_entry_id, state, created_at, updated_at)
SELECT p.id, p.company_id, p.id, p.vendor_bill_id, p.amount,
       COALESCE((SELECT ABS(SUM(ji.debit - ji.credit)) FROM journal_items ji
                 JOIN accounts a ON a.id = ji.account_id
                 WHERE ji.journal_entry_id = p.journal_entry_id AND a.type = 'PAYABLE'),
                ROUND(p.amount * p.exchange_rate_to_company, 4)),
       COALESCE((SELECT ABS(SUM(ji.debit - ji.credit)) FROM journal_items ji
                 JOIN accounts a ON a.id = ji.account_id
                 WHERE ji.journal_entry_id = p.journal_entry_id AND a.type = 'PAYABLE'),
                ROUND(p.amount * p.exchange_rate_to_company, 4)),
       CAST(p.payment_date AS DATE), NULL, NULL,
       CASE WHEN p.state = 'POSTED' THEN 'ACTIVE' ELSE 'REVERSED' END,
       p.created_at, p.updated_at
FROM pur_vendor_payment p
JOIN pur_vendor_bill b ON b.id = p.vendor_bill_id
WHERE p.state <> 'DRAFT'
  AND UPPER(b.currency_code) = UPPER(p.currency_code);

DROP INDEX ix_acc_cp_invoice ON acc_customer_payment;
ALTER TABLE acc_customer_payment DROP COLUMN customer_invoice_id;
DROP INDEX ix_pur_vp_bill ON pur_vendor_payment;
ALTER TABLE pur_vendor_payment DROP COLUMN vendor_bill_id;

ALTER TABLE acc_customer_invoice ADD COLUMN opening_balance BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pur_vendor_bill ADD COLUMN opening_balance BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE acc_customer_payment ADD COLUMN opening_balance BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pur_vendor_payment ADD COLUMN opening_balance BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX ix_acc_cp_partner ON acc_customer_payment(company_id, customer_partner_id);
CREATE INDEX ix_pur_vp_partner ON pur_vendor_payment(company_id, vendor_partner_id);

-- Receivable/payable reconciliation tags are re-derived from allocations once per company and side
-- at startup (TradeReconciliationRebuildRunner); this table records completed rebuilds.
CREATE TABLE acc_trade_reconciliation_rebuild (
    company_id CHAR(36) NOT NULL,
    side VARCHAR(16) NOT NULL,
    rebuilt_at TIMESTAMP NOT NULL,
    PRIMARY KEY (company_id, side)
);
