-- Expense types (find-or-create catalog) + link on expenses

CREATE TABLE IF NOT EXISTS exp_expense_type (
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX ix_exp_expense_type_company ON exp_expense_type(company_id);
CREATE UNIQUE INDEX ux_exp_expense_type_company_name ON exp_expense_type(company_id, name);

ALTER TABLE exp_expense ADD COLUMN expense_type_id UUID;
CREATE INDEX ix_exp_expense_type ON exp_expense(expense_type_id);
