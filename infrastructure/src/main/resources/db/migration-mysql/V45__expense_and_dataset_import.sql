-- Expenses (missing from earlier MySQL migration set)

CREATE TABLE IF NOT EXISTS exp_expense (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id CHAR(36),
    account_id CHAR(36),
    employee_id CHAR(36) NOT NULL,
    manager_employee_id CHAR(36),
    expense_date DATE NOT NULL,
    total DECIMAL(19, 4) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    currency_code VARCHAR(8) NOT NULL,
    reimbursement VARCHAR(32) NOT NULL DEFAULT 'EMPLOYEE',
    notes VARCHAR(2000),
    state VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    journal_entry_id CHAR(36),
    payment_journal_entry_id CHAR(36),
    payment_journal_id CHAR(36),
    amount_paid DECIMAL(19, 4) NOT NULL DEFAULT 0,
    payment_date DATE,
    payment_reference VARCHAR(255),
    expense_type_id CHAR(36),
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_exp_expense_employee FOREIGN KEY (employee_id) REFERENCES hr_employee(id)
);

CREATE INDEX ix_exp_expense_company ON exp_expense(company_id);
CREATE INDEX ix_exp_expense_company_state ON exp_expense(company_id, state);
CREATE INDEX ix_exp_expense_company_employee ON exp_expense(company_id, employee_id);
CREATE INDEX ix_exp_expense_date ON exp_expense(company_id, expense_date);
CREATE INDEX ix_exp_expense_type ON exp_expense(expense_type_id);

CREATE TABLE IF NOT EXISTS exp_expense_type (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE INDEX ix_exp_expense_type_company ON exp_expense_type(company_id);
CREATE UNIQUE INDEX ux_exp_expense_type_company_name ON exp_expense_type(company_id, name);

CREATE TABLE IF NOT EXISTS dataset_import_ref (
    company_id CHAR(36) NOT NULL,
    ref_type VARCHAR(32) NOT NULL,
    code VARCHAR(64) NOT NULL,
    entity_id CHAR(36) NOT NULL,
    PRIMARY KEY (company_id, ref_type, code)
);

CREATE INDEX ix_dataset_import_ref_entity ON dataset_import_ref (entity_id);
