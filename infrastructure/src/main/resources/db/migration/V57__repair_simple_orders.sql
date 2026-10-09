-- Repair simplified: one repair order (customer, product to repair, warranty flag) with its parts.
-- Confirming an order that is not under warranty creates a sale quotation; its id and name are kept on the order.
-- The phase 1 workshop tables (labor guide, packages, inspections, findings, lines) are no longer used.

DROP TABLE IF EXISTS rep_line;
DROP TABLE IF EXISTS rep_finding;
DROP TABLE IF EXISTS rep_inspection_result;
DROP TABLE IF EXISTS rep_inspection;
DROP TABLE IF EXISTS rep_inspection_template_item;
DROP TABLE IF EXISTS rep_inspection_template;
DROP TABLE IF EXISTS rep_package_line;
DROP TABLE IF EXISTS rep_package;
DROP TABLE IF EXISTS rep_labor_guide;
DROP TABLE IF EXISTS rep_labor_category;
DROP TABLE IF EXISTS rep_settings;

CREATE TABLE IF NOT EXISTS rep_order(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    reference VARCHAR(50) NOT NULL,
    customer_partner_id UUID,
    product_id UUID,
    scheduled_date TIMESTAMP,
    under_warranty BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    sale_order_id UUID,
    sale_order_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_order_company ON rep_order(company_id, created_at);

CREATE TABLE IF NOT EXISTS rep_order_part(
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    sequence INTEGER NOT NULL,
    product_id UUID NOT NULL,
    qty DECIMAL(19, 4) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_order_part_order ON rep_order_part(order_id);
