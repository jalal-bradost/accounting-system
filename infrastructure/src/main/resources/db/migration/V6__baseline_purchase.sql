-- Flyway baseline: purchase

CREATE TABLE IF NOT EXISTS pur_fiscal_tax(
    active BOOLEAN NOT NULL,
    amount NUMERIC(19, 6) NOT NULL,
    price_include BOOLEAN NOT NULL,
    account_id UUID NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    refund_account_id UUID,
    name VARCHAR(255) NOT NULL,
    amount_type VARCHAR(64) NOT NULL,
    scope VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pur_purchase_order(
    amount_tax NUMERIC(19, 4) NOT NULL,
    amount_total NUMERIC(19, 4) NOT NULL,
    amount_untaxed NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate_to_company NUMERIC(19, 8),
    expected_date DATE,
    order_date DATE,
    billed_completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    received_completed_at TIMESTAMP,
    row_version BIGINT NOT NULL,
    sent_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    dest_location_id UUID,
    id UUID NOT NULL,
    payment_terms_id UUID,
    vendor_partner_id UUID NOT NULL,
    warehouse_id UUID,
    incoterm VARCHAR(32),
    name VARCHAR(64) NOT NULL,
    notes VARCHAR(4000),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    vendor_reference VARCHAR(255),
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pur_purchase_order_line(
    discount_percent NUMERIC(19, 4) NOT NULL,
    expected_date DATE,
    qty_invoiced NUMERIC(19, 4) NOT NULL,
    qty_ordered NUMERIC(19, 4) NOT NULL,
    qty_received NUMERIC(19, 4) NOT NULL,
    sequence INTEGER NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    id UUID NOT NULL,
    product_id UUID,
    purchase_order_id UUID NOT NULL,
    uom_id UUID NOT NULL,
    warehouse_id UUID,
    name VARCHAR(512) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pur_purchase_order_line_tax(
    sequence INTEGER NOT NULL,
    id UUID NOT NULL,
    line_id UUID NOT NULL,
    tax_id UUID NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pur_vendor_bill(
    bill_date DATE NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    due_date DATE,
    exchange_rate_to_company NUMERIC(19, 8),
    created_at TIMESTAMP NOT NULL,
    row_version BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_entry_id UUID,
    purchase_order_id UUID,
    vendor_partner_id UUID NOT NULL,
    created_by VARCHAR(255),
    reference VARCHAR(255),
    updated_by VARCHAR(255),
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pur_vendor_bill_line(
    qty NUMERIC(19, 4) NOT NULL,
    sequence INTEGER NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    account_id UUID NOT NULL,
    id UUID NOT NULL,
    product_id UUID,
    purchase_order_line_id UUID,
    uom_id UUID NOT NULL,
    vendor_bill_id UUID NOT NULL,
    name VARCHAR(512) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS pur_vendor_bill_line_tax(
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

CREATE TABLE IF NOT EXISTS pur_vendor_payment(
    amount NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate_to_company NUMERIC(19, 12) NOT NULL,
    payment_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    bank_journal_id UUID NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_entry_id UUID,
    vendor_bill_id UUID NOT NULL,
    vendor_partner_id UUID NOT NULL,
    reference VARCHAR(255),
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pur_tax_company ON pur_fiscal_tax(company_id, active);

CREATE INDEX ix_pur_po_company_state ON pur_purchase_order(company_id, state);

CREATE INDEX ix_pur_po_vendor ON pur_purchase_order(vendor_partner_id, order_date);

CREATE INDEX ix_pur_pol_order ON pur_purchase_order_line(purchase_order_id);

CREATE INDEX ix_pur_vb_company_state ON pur_vendor_bill(company_id, state);

CREATE INDEX ix_pur_vb_po ON pur_vendor_bill(purchase_order_id);

CREATE INDEX ix_pur_vbl_bill ON pur_vendor_bill_line(vendor_bill_id);

CREATE INDEX ix_pur_vbl_pol ON pur_vendor_bill_line(purchase_order_line_id);

CREATE INDEX ix_pur_vp_company ON pur_vendor_payment(company_id, state);

CREATE INDEX ix_pur_vp_bill ON pur_vendor_payment(vendor_bill_id);

ALTER TABLE pur_purchase_order ADD CONSTRAINT uk_pur_po_company_name UNIQUE (company_id, name);

ALTER TABLE pur_vendor_bill_line ADD CONSTRAINT fk_pur_vbl_bill FOREIGN KEY(vendor_bill_id) REFERENCES pur_vendor_bill(id);

ALTER TABLE pur_vendor_bill_line_tax ADD CONSTRAINT fk_pur_vblt_line FOREIGN KEY(line_id) REFERENCES pur_vendor_bill_line(id);

ALTER TABLE pur_purchase_order_line ADD CONSTRAINT fk_pur_pol_order FOREIGN KEY(purchase_order_id) REFERENCES pur_purchase_order(id);

ALTER TABLE pur_purchase_order_line_tax ADD CONSTRAINT fk_pur_polt_line FOREIGN KEY(line_id) REFERENCES pur_purchase_order_line(id);
