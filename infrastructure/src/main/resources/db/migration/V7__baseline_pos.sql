-- Flyway baseline: pos

CREATE TABLE IF NOT EXISTS pos_config(
    active BOOLEAN NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    bank_journal_id UUID,
    cash_journal_id UUID NOT NULL,
    company_id UUID NOT NULL,
    default_customer_partner_id UUID NOT NULL,
    id UUID NOT NULL,
    pricelist_id UUID,
    warehouse_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pos_order(
    amount_paid NUMERIC(19, 4) NOT NULL,
    amount_tax NUMERIC(19, 4) NOT NULL,
    amount_total NUMERIC(19, 4) NOT NULL,
    amount_untaxed NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    finalized_at TIMESTAMP,
    row_version BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    customer_invoice_id UUID,
    customer_partner_id UUID NOT NULL,
    id UUID NOT NULL,
    receipt_id UUID,
    sales_order_id UUID,
    session_id UUID NOT NULL,
    name VARCHAR(64) NOT NULL,
    note VARCHAR(4000),
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pos_order_line(
    discount_percent NUMERIC(9, 4) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    sequence INTEGER NOT NULL,
    subtotal NUMERIC(19, 4) NOT NULL,
    tax_amount NUMERIC(19, 4) NOT NULL,
    total NUMERIC(19, 4) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    revenue_account_id UUID,
    uom_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pos_order_line_tax(
    sequence INTEGER NOT NULL,
    line_id UUID NOT NULL,
    tax_id UUID NOT NULL
,
    PRIMARY KEY (sequence, line_id)
);

CREATE TABLE IF NOT EXISTS pos_payment(
    amount NUMERIC(19, 4) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    id UUID NOT NULL,
    journal_id UUID NOT NULL,
    order_id UUID NOT NULL,
    reference VARCHAR(255),
    method VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pos_receipt(
    created_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    receipt_number VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pos_session(
    closing_cash NUMERIC(19, 4),
    currency_code VARCHAR(3) NOT NULL,
    opening_cash NUMERIC(19, 4) NOT NULL,
    closed_at TIMESTAMP,
    opened_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL,
    bank_journal_id UUID,
    cash_journal_id UUID NOT NULL,
    company_id UUID NOT NULL,
    config_id UUID NOT NULL,
    default_customer_partner_id UUID NOT NULL,
    id UUID NOT NULL,
    pricelist_id UUID,
    warehouse_id UUID NOT NULL,
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pos_config_company_active ON pos_config(company_id, active);

CREATE INDEX ix_pos_order_company_state ON pos_order(company_id, state);

CREATE INDEX ix_pos_order_session ON pos_order(session_id);

CREATE INDEX ix_pos_receipt_company ON pos_receipt(company_id);

CREATE INDEX ix_pos_receipt_order ON pos_receipt(order_id);

CREATE INDEX ix_pos_session_company_state ON pos_session(company_id, state);

CREATE INDEX ix_pos_session_config ON pos_session(config_id);

ALTER TABLE pos_config ADD CONSTRAINT uk_pos_config_company_name UNIQUE (company_id, name);

ALTER TABLE pos_receipt ADD CONSTRAINT uk_pos_receipt_company_number UNIQUE (company_id, receipt_number);

ALTER TABLE pos_order ADD CONSTRAINT uk_pos_order_company_name UNIQUE (company_id, name);

ALTER TABLE pos_payment ADD CONSTRAINT fkoopv7u989q6b6v1sb0lkm7xhl FOREIGN KEY(order_id) REFERENCES pos_order(id);

ALTER TABLE pos_order_line_tax ADD CONSTRAINT fktr2x1qck675m1j4bwx06u6ba0 FOREIGN KEY(line_id) REFERENCES pos_order_line(id);

ALTER TABLE pos_order_line ADD CONSTRAINT fka3v11oqcxdctbcrl4hgxxxjfo FOREIGN KEY(order_id) REFERENCES pos_order(id);
