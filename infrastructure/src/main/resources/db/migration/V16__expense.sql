-- Flyway: Expenses (Odoo-style single expense claims)

CREATE TABLE IF NOT EXISTS exp_expense(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    product_id UUID,
    account_id UUID,
    employee_id UUID NOT NULL,
    manager_employee_id UUID,
    expense_date DATE NOT NULL,
    total DECIMAL(19, 4) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    currency_code VARCHAR(8) NOT NULL,
    reimbursement VARCHAR(32) NOT NULL DEFAULT 'EMPLOYEE',
    notes VARCHAR(2000),
    state VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    journal_entry_id UUID,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX ix_exp_expense_company ON exp_expense(company_id);
CREATE INDEX ix_exp_expense_company_state ON exp_expense(company_id, state);
CREATE INDEX ix_exp_expense_company_employee ON exp_expense(company_id, employee_id);
CREATE INDEX ix_exp_expense_date ON exp_expense(company_id, expense_date);

ALTER TABLE exp_expense ADD CONSTRAINT fk_exp_expense_employee
    FOREIGN KEY(employee_id) REFERENCES hr_employee(id);
