-- Flyway baseline: inventory

CREATE TABLE IF NOT EXISTS inv_product(
    active BOOLEAN NOT NULL,
    list_price NUMERIC(19, 4) NOT NULL,
    purchase_ok BOOLEAN NOT NULL,
    sale_ok BOOLEAN NOT NULL,
    standard_cost NUMERIC(19, 4) NOT NULL,
    archived_at TIMESTAMP,
    category_id UUID NOT NULL,
    cogs_account_id_override UUID,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    purchase_uom_id UUID,
    stock_input_account_id_override UUID,
    stock_output_account_id_override UUID,
    stock_valuation_account_id_override UUID,
    uom_id UUID NOT NULL,
    barcode VARCHAR(100),
    image_content_type VARCHAR(100),
    sku VARCHAR(100) NOT NULL,
    image_url VARCHAR(512),
    description VARCHAR(1000),
    archived_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    product_type VARCHAR(64) NOT NULL,
    valuation_method_override VARCHAR(64)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_product_category(
    active BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    cogs_account_id UUID,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    parent_id UUID,
    stock_input_account_id UUID,
    stock_output_account_id UUID,
    stock_valuation_account_id UUID,
    archived_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    valuation_method VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_stock_location(
    active BOOLEAN NOT NULL,
    allow_negative_stock BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    parent_id UUID,
    warehouse_id UUID,
    code VARCHAR(100) NOT NULL,
    archived_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    location_type VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_stock_move(
    demand_quantity NUMERIC(19, 4) NOT NULL,
    picked_quantity NUMERIC(19, 4) NOT NULL,
    reserved_quantity NUMERIC(19, 4) NOT NULL,
    unit_cost NUMERIC(19, 4) NOT NULL,
    destination_location_id UUID NOT NULL,
    id UUID NOT NULL,
    picking_id UUID NOT NULL,
    product_id UUID NOT NULL,
    purchase_order_line_id UUID,
    sales_order_line_id UUID,
    source_location_id UUID NOT NULL,
    uom_id UUID NOT NULL,
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_stock_picking(
    scheduled_at TIMESTAMP,
    validated_at TIMESTAMP,
    backorder_of UUID,
    company_id UUID NOT NULL,
    destination_location_id UUID NOT NULL,
    id UUID NOT NULL,
    partner_id UUID,
    purchase_order_id UUID,
    sales_order_id UUID,
    source_location_id UUID NOT NULL,
    warehouse_id UUID,
    reference VARCHAR(100),
    origin VARCHAR(255),
    validated_by VARCHAR(255),
    picking_type VARCHAR(64) NOT NULL,
    state VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_stock_quant(
    quantity NUMERIC(19, 4) NOT NULL,
    reserved_quantity NUMERIC(19, 4) NOT NULL,
    last_changed_at TIMESTAMP,
    version BIGINT NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    location_id UUID NOT NULL,
    product_id UUID NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_stock_valuation_layer(
    quantity NUMERIC(19, 4) NOT NULL,
    remaining_quantity NUMERIC(19, 4) NOT NULL,
    remaining_value NUMERIC(19, 4) NOT NULL,
    total_value NUMERIC(19, 4) NOT NULL,
    unit_cost NUMERIC(19, 4) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    journal_entry_id UUID,
    product_id UUID NOT NULL,
    stock_move_id UUID,
    method VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_uom(
    active BOOLEAN NOT NULL,
    factor NUMERIC(19, 6) NOT NULL,
    rounding INTEGER NOT NULL,
    archived_at TIMESTAMP,
    category_id UUID NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    archived_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    uom_type VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_uom_category(
    active BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    archived_by VARCHAR(255),
    name VARCHAR(255) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inv_warehouse(
    active BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    input_location_id UUID,
    output_location_id UUID,
    stock_location_id UUID,
    code VARCHAR(50) NOT NULL,
    archived_by VARCHAR(255),
    name VARCHAR(255) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE INDEX ix_inv_product_company ON inv_product(company_id, active);

CREATE INDEX ix_inv_product_company_sku ON inv_product(company_id, sku);

CREATE INDEX ix_inv_product_company_name ON inv_product(company_id, name);

CREATE INDEX ix_inv_pcat_company ON inv_product_category(company_id, active);

CREATE INDEX ix_inv_loc_company ON inv_stock_location(company_id, active);

CREATE INDEX ix_inv_loc_warehouse ON inv_stock_location(warehouse_id, active);

CREATE INDEX ix_inv_loc_type ON inv_stock_location(company_id, location_type);

CREATE INDEX ix_inv_move_picking ON inv_stock_move(picking_id);

CREATE INDEX ix_inv_move_product ON inv_stock_move(product_id);

CREATE INDEX ix_inv_move_state ON inv_stock_move(state);

CREATE INDEX ix_inv_move_po_line ON inv_stock_move(purchase_order_line_id);

CREATE INDEX ix_inv_move_so_line ON inv_stock_move(sales_order_line_id);

CREATE INDEX ix_inv_pick_company ON inv_stock_picking(company_id);

CREATE INDEX ix_inv_pick_company_state ON inv_stock_picking(company_id, state);

CREATE INDEX ix_inv_pick_company_type ON inv_stock_picking(company_id, picking_type);

CREATE INDEX ix_inv_pick_partner ON inv_stock_picking(partner_id);

CREATE INDEX ix_inv_pick_purchase_order ON inv_stock_picking(purchase_order_id);

CREATE INDEX ix_inv_quant_company_product ON inv_stock_quant(company_id, product_id);

CREATE INDEX ix_inv_quant_company_location ON inv_stock_quant(company_id, location_id);

CREATE INDEX ix_inv_svl_company_product ON inv_stock_valuation_layer(company_id, product_id);

CREATE INDEX ix_inv_svl_fifo ON inv_stock_valuation_layer(company_id, product_id, occurred_at);

CREATE INDEX ix_inv_svl_journal ON inv_stock_valuation_layer(journal_entry_id);

CREATE INDEX ix_inv_uom_company ON inv_uom(company_id, active);

CREATE INDEX ix_inv_uom_category ON inv_uom(category_id, active);

CREATE INDEX ix_inv_uomcat_company ON inv_uom_category(company_id, active);

CREATE INDEX ix_inv_wh_company ON inv_warehouse(company_id, active);

CREATE INDEX ix_inv_wh_company_code ON inv_warehouse(company_id, code);

ALTER TABLE inv_stock_quant ADD CONSTRAINT uk_inv_quant_clp UNIQUE (company_id, product_id, location_id);

ALTER TABLE inv_stock_move ADD CONSTRAINT fk_inv_move_picking FOREIGN KEY(picking_id) REFERENCES inv_stock_picking(id);
