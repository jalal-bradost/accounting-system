-- Flyway: Payroll (Iraq-focused MVP)

CREATE TABLE IF NOT EXISTS pay_working_schedule(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    two_week_calendar BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_working_schedule_company ON pay_working_schedule(company_id);

CREATE TABLE IF NOT EXISTS pay_working_schedule_line(
    id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL,
    hours DECIMAL(5, 2) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_working_schedule_line_schedule ON pay_working_schedule_line(schedule_id);

ALTER TABLE pay_working_schedule_line ADD CONSTRAINT fk_pay_schedule_line_schedule
    FOREIGN KEY(schedule_id) REFERENCES pay_working_schedule(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS pay_employee_type(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    country_code VARCHAR(8),
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_employee_type_company ON pay_employee_type(company_id);

CREATE TABLE IF NOT EXISTS pay_structure_type(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    scheduled_pay VARCHAR(32) NOT NULL DEFAULT 'month',
    wage_type VARCHAR(32) NOT NULL DEFAULT 'fixed',
    working_schedule_id UUID,
    country_code VARCHAR(8),
    pay_structure_id UUID,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_structure_type_company ON pay_structure_type(company_id);

ALTER TABLE pay_structure_type ADD CONSTRAINT fk_pay_structure_type_schedule
    FOREIGN KEY(working_schedule_id) REFERENCES pay_working_schedule(id);

CREATE TABLE IF NOT EXISTS pay_structure(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    structure_type_id UUID,
    scheduled_pay VARCHAR(32) NOT NULL DEFAULT 'month',
    use_worked_day_lines BOOLEAN NOT NULL DEFAULT TRUE,
    country_code VARCHAR(8),
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_structure_company ON pay_structure(company_id);

ALTER TABLE pay_structure ADD CONSTRAINT fk_pay_structure_type
    FOREIGN KEY(structure_type_id) REFERENCES pay_structure_type(id);

ALTER TABLE pay_structure_type ADD CONSTRAINT fk_pay_structure_type_structure
    FOREIGN KEY(pay_structure_id) REFERENCES pay_structure(id);

CREATE TABLE IF NOT EXISTS pay_salary_rule(
    id UUID NOT NULL,
    structure_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(32) NOT NULL,
    category VARCHAR(32) NOT NULL,
    amount_type VARCHAR(32) NOT NULL DEFAULT 'fixed',
    amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    sequence INTEGER NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_salary_rule_structure ON pay_salary_rule(structure_id);

ALTER TABLE pay_salary_rule ADD CONSTRAINT fk_pay_salary_rule_structure
    FOREIGN KEY(structure_id) REFERENCES pay_structure(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS pay_contract(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    employee_type_id UUID,
    structure_id UUID NOT NULL,
    working_schedule_id UUID NOT NULL,
    wage DECIMAL(19, 4) NOT NULL,
    wage_type VARCHAR(32) NOT NULL DEFAULT 'fixed',
    currency_code VARCHAR(8) NOT NULL DEFAULT 'IQD',
    date_start DATE NOT NULL,
    date_end DATE,
    state VARCHAR(32) NOT NULL DEFAULT 'draft',
    attendance_based BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_contract_company ON pay_contract(company_id);
CREATE INDEX ix_pay_contract_employee ON pay_contract(employee_id);

ALTER TABLE pay_contract ADD CONSTRAINT fk_pay_contract_employee
    FOREIGN KEY(employee_id) REFERENCES hr_employee(id);

ALTER TABLE pay_contract ADD CONSTRAINT fk_pay_contract_structure
    FOREIGN KEY(structure_id) REFERENCES pay_structure(id);

ALTER TABLE pay_contract ADD CONSTRAINT fk_pay_contract_schedule
    FOREIGN KEY(working_schedule_id) REFERENCES pay_working_schedule(id);

ALTER TABLE pay_contract ADD CONSTRAINT fk_pay_contract_employee_type
    FOREIGN KEY(employee_type_id) REFERENCES pay_employee_type(id);
