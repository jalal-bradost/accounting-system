-- Flyway: HR Time Off (Odoo-style, no compensatory time off)

CREATE TABLE IF NOT EXISTS hr_time_off_type(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(32) NOT NULL,
    display_code VARCHAR(16) NOT NULL,
    country_code VARCHAR(8),
    color_hex VARCHAR(16) NOT NULL DEFAULT '#714B67',
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ux_hr_time_off_type_company_code ON hr_time_off_type(company_id, code);
CREATE INDEX ix_hr_time_off_type_company ON hr_time_off_type(company_id);

CREATE TABLE IF NOT EXISTS hr_time_off_allocation(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    time_off_type_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    number_of_days DECIMAL(8, 2) NOT NULL,
    allocation_type VARCHAR(32) NOT NULL DEFAULT 'regular',
    state VARCHAR(32) NOT NULL DEFAULT 'confirm',
    date_from DATE NOT NULL,
    date_to DATE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX ix_hr_time_off_allocation_company ON hr_time_off_allocation(company_id);
CREATE INDEX ix_hr_time_off_allocation_employee ON hr_time_off_allocation(employee_id);

ALTER TABLE hr_time_off_allocation ADD CONSTRAINT fk_hr_time_off_allocation_employee
    FOREIGN KEY(employee_id) REFERENCES hr_employee(id);
ALTER TABLE hr_time_off_allocation ADD CONSTRAINT fk_hr_time_off_allocation_type
    FOREIGN KEY(time_off_type_id) REFERENCES hr_time_off_type(id);

CREATE TABLE IF NOT EXISTS hr_time_off_request(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    time_off_type_id UUID NOT NULL,
    date_from DATE NOT NULL,
    date_to DATE NOT NULL,
    number_of_days DECIMAL(8, 2) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'confirm',
    description VARCHAR(512),
    PRIMARY KEY (id)
);

CREATE INDEX ix_hr_time_off_request_company ON hr_time_off_request(company_id);
CREATE INDEX ix_hr_time_off_request_employee ON hr_time_off_request(employee_id);
CREATE INDEX ix_hr_time_off_request_dates ON hr_time_off_request(company_id, date_from, date_to);

ALTER TABLE hr_time_off_request ADD CONSTRAINT fk_hr_time_off_request_employee
    FOREIGN KEY(employee_id) REFERENCES hr_employee(id);
ALTER TABLE hr_time_off_request ADD CONSTRAINT fk_hr_time_off_request_type
    FOREIGN KEY(time_off_type_id) REFERENCES hr_time_off_type(id);

CREATE TABLE IF NOT EXISTS hr_public_holiday(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    holiday_date DATE NOT NULL,
    country_code VARCHAR(8),
    PRIMARY KEY (id)
);

CREATE INDEX ix_hr_public_holiday_company ON hr_public_holiday(company_id);

CREATE TABLE IF NOT EXISTS hr_mandatory_day(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    mandatory_date DATE NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX ix_hr_mandatory_day_company ON hr_mandatory_day(company_id);
