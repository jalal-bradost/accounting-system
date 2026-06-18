-- Flyway baseline: accounting

CREATE TABLE IF NOT EXISTS acc_customer_invoice(
    currency_code VARCHAR(3) NOT NULL,
    due_date DATE,
    exchange_rate_to_company NUMERIC(19, 8),
    invoice_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    customer_partner_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_entry_id UUID,
    sales_order_id UUID,
    reference VARCHAR(255),
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS acc_customer_invoice_line(
    discount_percent NUMERIC(19, 4) NOT NULL,
    qty NUMERIC(19, 4) NOT NULL,
    sequence INTEGER NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    customer_invoice_id UUID NOT NULL,
    id UUID NOT NULL,
    revenue_account_id UUID NOT NULL,
    sales_order_line_id UUID,
    name VARCHAR(512) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS acc_customer_invoice_line_tax(
    tax_amount NUMERIC(19, 4) NOT NULL,
    tax_base NUMERIC(19, 4) NOT NULL,
    account_id UUID NOT NULL,
    id UUID NOT NULL,
    line_id UUID NOT NULL,
    tax_id UUID NOT NULL,
    tax_name VARCHAR(255) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS acc_customer_payment(
    amount NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate_to_company NUMERIC(19, 12) NOT NULL,
    payment_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    customer_invoice_id UUID NOT NULL,
    customer_partner_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_entry_id UUID,
    payment_journal_id UUID NOT NULL,
    reference VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS accounts(
    active BOOLEAN NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS company_currencies(
    active BOOLEAN NOT NULL,
    base_currency BOOLEAN NOT NULL,
    code VARCHAR(3) NOT NULL,
    last_rate_updated DATE,
    rate_per_base NUMERIC(19, 6) NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    name VARCHAR(200) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS company_settings(
    period_lock_date DATE,
    company_id UUID NOT NULL
,
    PRIMARY KEY (company_id)
);

CREATE TABLE IF NOT EXISTS currency_rates(
    effective_date DATE NOT NULL,
    rate NUMERIC(19, 6) NOT NULL,
    currency_id UUID NOT NULL,
    id UUID NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS fiscal_periods(
    end_date DATE NOT NULL,
    open BOOLEAN NOT NULL,
    start_date DATE NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS journal_entries(
    currency_code VARCHAR(3),
    entry_date DATE NOT NULL,
    created_at TIMESTAMP,
    posted_at TIMESTAMP,
    updated_at TIMESTAMP,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_id UUID NOT NULL,
    partner_id UUID,
    reversal_of_entry_id UUID,
    partner_name VARCHAR(255),
    posted_by VARCHAR(255),
    sequence_number VARCHAR(255) NOT NULL,
    status VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS journal_entry_sequences(
    last_number BIGINT NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_id UUID NOT NULL,
    period_key VARCHAR(20) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS journal_items(
    amount_currency NUMERIC(19, 4),
    credit NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3),
    debit NUMERIC(19, 4) NOT NULL,
    account_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_entry_id UUID NOT NULL,
    partner_id UUID,
    reconciliation_id UUID,
    label VARCHAR(500),
    partner_name VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS journals(
    code VARCHAR(10) NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE INDEX ix_acc_ci_company_state ON acc_customer_invoice(company_id, state);

CREATE INDEX ix_acc_ci_partner ON acc_customer_invoice(customer_partner_id);

CREATE INDEX ix_acc_cil_invoice ON acc_customer_invoice_line(customer_invoice_id);

CREATE INDEX ix_acc_cp_company ON acc_customer_payment(company_id);

CREATE INDEX ix_acc_cp_invoice ON acc_customer_payment(customer_invoice_id);

ALTER TABLE journals ADD CONSTRAINT uk_journals_company_id_code UNIQUE (company_id, code);

ALTER TABLE journal_entry_sequences ADD CONSTRAINT uk_journal_entry_sequences_company_id_journal_id_period_key UNIQUE (company_id, journal_id, period_key);

ALTER TABLE company_currencies ADD CONSTRAINT uk_company_currencies_company_id_code UNIQUE (company_id, code);

ALTER TABLE accounts ADD CONSTRAINT uk_accounts_company_id_code UNIQUE (company_id, code);

ALTER TABLE currency_rates ADD CONSTRAINT uk_currency_rates_currency_id_effective_date UNIQUE (currency_id, effective_date);

ALTER TABLE acc_customer_invoice_line ADD CONSTRAINT fk_acc_cil_invoice FOREIGN KEY(customer_invoice_id) REFERENCES acc_customer_invoice(id);

ALTER TABLE journal_items ADD CONSTRAINT fkf61riqeicfoj71l9f2h7f0vum FOREIGN KEY(account_id) REFERENCES accounts(id);

ALTER TABLE acc_customer_invoice_line_tax ADD CONSTRAINT fk_acc_cilt_line FOREIGN KEY(line_id) REFERENCES acc_customer_invoice_line(id);

ALTER TABLE journal_entries ADD CONSTRAINT fkaeg8fn0yg6nexouwe3amy7qx1 FOREIGN KEY(journal_id) REFERENCES journals(id);

ALTER TABLE journal_items ADD CONSTRAINT fkb53gc3vpiwwftjjmvbg1mkf6q FOREIGN KEY(journal_entry_id) REFERENCES journal_entries(id);
