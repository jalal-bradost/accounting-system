CREATE TABLE IF NOT EXISTS pos_config (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    warehouse_id UUID NOT NULL,
    default_customer_partner_id UUID NOT NULL,
    cash_journal_id UUID NOT NULL,
    bank_journal_id UUID,
    pricelist_id UUID,
    currency_code VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_pos_config_company_name UNIQUE (company_id, name)
);

CREATE INDEX IF NOT EXISTS ix_pos_config_company_active ON pos_config (company_id, active);

CREATE TABLE IF NOT EXISTS pos_session (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    config_id UUID NOT NULL,
    state VARCHAR(32) NOT NULL,
    warehouse_id UUID NOT NULL,
    default_customer_partner_id UUID NOT NULL,
    cash_journal_id UUID NOT NULL,
    bank_journal_id UUID,
    pricelist_id UUID,
    currency_code VARCHAR(3) NOT NULL,
    opening_cash NUMERIC(19, 4) NOT NULL,
    closing_cash NUMERIC(19, 4),
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    row_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_pos_session_company_state ON pos_session (company_id, state);
CREATE INDEX IF NOT EXISTS ix_pos_session_config ON pos_session (config_id);

CREATE TABLE IF NOT EXISTS pos_order (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    session_id UUID NOT NULL,
    customer_partner_id UUID NOT NULL,
    name VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    amount_untaxed NUMERIC(19, 4) NOT NULL,
    amount_tax NUMERIC(19, 4) NOT NULL,
    amount_total NUMERIC(19, 4) NOT NULL,
    amount_paid NUMERIC(19, 4) NOT NULL,
    note VARCHAR(4000),
    sales_order_id UUID,
    customer_invoice_id UUID,
    receipt_id UUID,
    finalized_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_pos_order_company_name UNIQUE (company_id, name)
);

CREATE INDEX IF NOT EXISTS ix_pos_order_company_state ON pos_order (company_id, state);
CREATE INDEX IF NOT EXISTS ix_pos_order_session ON pos_order (session_id);

CREATE TABLE IF NOT EXISTS pos_order_line (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    sequence INTEGER NOT NULL,
    product_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    uom_id UUID NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    discount_percent NUMERIC(9, 4) NOT NULL,
    subtotal NUMERIC(19, 4) NOT NULL,
    tax_amount NUMERIC(19, 4) NOT NULL,
    total NUMERIC(19, 4) NOT NULL,
    revenue_account_id UUID,
    CONSTRAINT fk_pos_order_line_order FOREIGN KEY (order_id) REFERENCES pos_order(id)
);

CREATE TABLE IF NOT EXISTS pos_order_line_tax (
    line_id UUID NOT NULL,
    sequence INTEGER NOT NULL,
    tax_id UUID NOT NULL,
    CONSTRAINT fk_pos_order_line_tax_line FOREIGN KEY (line_id) REFERENCES pos_order_line(id)
);

CREATE TABLE IF NOT EXISTS pos_payment (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    method VARCHAR(32) NOT NULL,
    journal_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    reference VARCHAR(255),
    paid_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pos_payment_order FOREIGN KEY (order_id) REFERENCES pos_order(id)
);

CREATE TABLE IF NOT EXISTS pos_receipt (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    order_id UUID NOT NULL,
    receipt_number VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_pos_receipt_company_number UNIQUE (company_id, receipt_number)
);

CREATE INDEX IF NOT EXISTS ix_pos_receipt_company ON pos_receipt (company_id);
CREATE INDEX IF NOT EXISTS ix_pos_receipt_order ON pos_receipt (order_id);
