-- Repair phase 1 (foundation): settings, labor guide, packages, inspection, findings and repair lines.
-- order_id points at the future maintenance order (epic RCP); it is an opaque id until that module exists.

CREATE TABLE IF NOT EXISTS rep_settings(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    default_hourly_rate DECIMAL(19, 4),
    verbal_limit_advisor DECIMAL(19, 4) NOT NULL,
    verbal_limit_supervisor DECIMAL(19, 4) NOT NULL,
    warranty_days INTEGER NOT NULL,
    warranty_km INTEGER NOT NULL,
    emergency_start_limit DECIMAL(19, 4) NOT NULL,
    quote_validity_days INTEGER NOT NULL,
    advisor_discount_limit_percent DECIMAL(9, 4) NOT NULL,
    comeback_days INTEGER NOT NULL,
    parts_tolerance_percent DECIMAL(9, 4) NOT NULL,
    default_inspection_template_id UUID,
    default_quality_template_id UUID,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX ux_rep_settings_company ON rep_settings(company_id);

CREATE TABLE IF NOT EXISTS rep_labor_category(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    hourly_rate DECIMAL(19, 4),
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_labor_category_company ON rep_labor_category(company_id);

CREATE TABLE IF NOT EXISTS rep_labor_guide(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    description_en VARCHAR(500),
    description_ar VARCHAR(500),
    description_ku VARCHAR(500),
    labor_category_id UUID,
    standard_minutes INTEGER NOT NULL,
    make VARCHAR(100),
    model VARCHAR(100),
    year_from INTEGER,
    year_to INTEGER,
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_labor_guide_company ON rep_labor_guide(company_id, code);

CREATE TABLE IF NOT EXISTS rep_package(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    code VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_package_company ON rep_package(company_id);

CREATE TABLE IF NOT EXISTS rep_package_line(
    id UUID NOT NULL,
    package_id UUID NOT NULL,
    sequence INTEGER NOT NULL,
    line_type VARCHAR(20) NOT NULL,
    product_id UUID,
    labor_guide_id UUID,
    description VARCHAR(500),
    qty DECIMAL(19, 4) NOT NULL,
    standard_minutes INTEGER,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_package_line_package ON rep_package_line(package_id);

CREATE TABLE IF NOT EXISTS rep_inspection_template(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    vehicle_type VARCHAR(50),
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_insp_template_company ON rep_inspection_template(company_id);

CREATE TABLE IF NOT EXISTS rep_inspection_template_item(
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    section VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_insp_template_item ON rep_inspection_template_item(template_id);

CREATE TABLE IF NOT EXISTS rep_inspection(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    order_id UUID NOT NULL,
    template_id UUID,
    performed_by VARCHAR(255),
    started_at TIMESTAMP NOT NULL,
    signed_off_at TIMESTAMP,
    signed_off_by VARCHAR(255),
    odometer_km INTEGER,
    notes VARCHAR(4000),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX ux_rep_inspection_order ON rep_inspection(order_id);

CREATE TABLE IF NOT EXISTS rep_inspection_result(
    id UUID NOT NULL,
    inspection_id UUID NOT NULL,
    sequence INTEGER NOT NULL,
    section VARCHAR(100) NOT NULL,
    item_label VARCHAR(255) NOT NULL,
    result VARCHAR(20) NOT NULL,
    note VARCHAR(2000),
    photo_document_id UUID,
    recommended_line_id UUID,
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_insp_result ON rep_inspection_result(inspection_id);

CREATE TABLE IF NOT EXISTS rep_finding(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    order_id UUID NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    cause VARCHAR(2000),
    recommended_action VARCHAR(2000),
    customer_visible BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_finding_order ON rep_finding(order_id);

CREATE TABLE IF NOT EXISTS rep_line(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    order_id UUID NOT NULL,
    line_type VARCHAR(20) NOT NULL,
    section_label VARCHAR(255),
    sequence INTEGER NOT NULL,
    product_id UUID,
    description VARCHAR(500) NOT NULL,
    qty DECIMAL(19, 4) NOT NULL,
    unit_price DECIMAL(19, 4) NOT NULL,
    discount_percent DECIMAL(9, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    needs_discount_approval BOOLEAN NOT NULL,
    from_finding_id UUID,
    from_package_id UUID,
    labor_guide_id UUID,
    pricing_mode VARCHAR(20),
    standard_minutes INTEGER,
    labor_rate DECIMAL(19, 4),
    assignee_employee_id UUID,
    vendor_partner_id UUID,
    vendor_cost DECIMAL(19, 4),
    sublet_status VARCHAR(20),
    disposition VARCHAR(30),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE INDEX ix_rep_line_order ON rep_line(order_id);
