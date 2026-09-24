-- Flyway baseline: sales

CREATE TABLE IF NOT EXISTS sal_pricelist(
    active BOOLEAN NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    sequence INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sal_pricelist_item(
    date_from DATE,
    date_to DATE,
    fixed_price NUMERIC(19, 4),
    min_quantity NUMERIC(19, 4) NOT NULL,
    percent_discount NUMERIC(19, 4),
    sequence INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    id UUID NOT NULL,
    pricelist_id UUID NOT NULL,
    product_id UUID
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sal_sales_order(
    amount_tax NUMERIC(19, 4) NOT NULL,
    amount_total NUMERIC(19, 4) NOT NULL,
    amount_untaxed NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate_to_company NUMERIC(19, 8),
    order_date DATE,
    validity_date DATE,
    cancelled_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    delivery_completed_at TIMESTAMP,
    invoicing_completed_at TIMESTAMP,
    quotation_sent_at TIMESTAMP,
    row_version BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    customer_partner_id UUID NOT NULL,
    id UUID NOT NULL,
    payment_terms_id UUID,
    pricelist_id UUID,
    warehouse_id UUID,
    incoterm VARCHAR(32),
    name VARCHAR(64) NOT NULL,
    notes VARCHAR(4000),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    delivery_status VARCHAR(64) NOT NULL,
    invoice_status VARCHAR(64) NOT NULL,
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sal_sales_order_line(
    discount_percent NUMERIC(19, 4) NOT NULL,
    qty_delivered NUMERIC(19, 4) NOT NULL,
    qty_invoiced NUMERIC(19, 4) NOT NULL,
    qty_ordered NUMERIC(19, 4) NOT NULL,
    sequence INTEGER NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    id UUID NOT NULL,
    product_id UUID NOT NULL,
    revenue_account_id UUID,
    sales_order_id UUID NOT NULL,
    uom_id UUID NOT NULL,
    name VARCHAR(512) NOT NULL,
    invoice_policy VARCHAR(64)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sal_sales_order_line_tax(
    sequence INTEGER NOT NULL,
    id UUID NOT NULL,
    line_id UUID NOT NULL,
    tax_id UUID NOT NULL
,
    PRIMARY KEY (id)
);

CREATE INDEX ix_sal_pl_company ON sal_pricelist(company_id);

CREATE INDEX ix_sal_pli_pl ON sal_pricelist_item(pricelist_id);

CREATE INDEX ix_sal_pli_product ON sal_pricelist_item(product_id);

CREATE INDEX ix_sal_so_company_state ON sal_sales_order(company_id, state);

CREATE INDEX ix_sal_so_customer ON sal_sales_order(customer_partner_id);

CREATE INDEX ix_sal_sol_order ON sal_sales_order_line(sales_order_id);

ALTER TABLE sal_sales_order ADD CONSTRAINT uk_sal_so_company_name UNIQUE (company_id, name);

ALTER TABLE sal_pricelist_item ADD CONSTRAINT fk_sal_pli_pl FOREIGN KEY(pricelist_id) REFERENCES sal_pricelist(id);

ALTER TABLE sal_sales_order_line ADD CONSTRAINT fk_sal_sol_order FOREIGN KEY(sales_order_id) REFERENCES sal_sales_order(id);

ALTER TABLE sal_sales_order_line_tax ADD CONSTRAINT fk_sal_solt_line FOREIGN KEY(line_id) REFERENCES sal_sales_order_line(id);
