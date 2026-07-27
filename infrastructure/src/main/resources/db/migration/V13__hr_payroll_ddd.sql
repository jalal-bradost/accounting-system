-- HR employee ↔ platform user link
ALTER TABLE hr_employee ADD COLUMN IF NOT EXISTS user_id UUID;

ALTER TABLE hr_employee ADD CONSTRAINT fk_hr_employee_user
    FOREIGN KEY(user_id) REFERENCES platform_app_user(id);

-- One linked user per company; NULL user_id rows are allowed (H2 treats NULLs as distinct in unique indexes).
CREATE UNIQUE INDEX IF NOT EXISTS ux_hr_employee_company_user
    ON hr_employee(company_id, user_id);

-- Salary rule GL account mapping
ALTER TABLE pay_salary_rule ADD COLUMN IF NOT EXISTS account_id UUID;

ALTER TABLE pay_salary_rule ADD CONSTRAINT fk_pay_salary_rule_account
    FOREIGN KEY(account_id) REFERENCES accounts(id);

-- Remove hardcoded IQD default on contracts
ALTER TABLE pay_contract ALTER COLUMN currency_code DROP DEFAULT;

UPDATE pay_contract pc
SET currency_code = COALESCE(
    (SELECT c.default_currency FROM platform_company c WHERE c.id = pc.company_id),
    (SELECT cc.code FROM company_currencies cc
     WHERE cc.company_id = pc.company_id AND cc.base_currency = TRUE AND cc.active = TRUE
     LIMIT 1),
    pc.currency_code
)
WHERE pc.currency_code = 'IQD';

-- Pay run execution
CREATE TABLE IF NOT EXISTS pay_run(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'draft',
    journal_entry_id UUID,
    payment_journal_entry_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_run_company ON pay_run(company_id);
CREATE INDEX ix_pay_run_period ON pay_run(company_id, period_start, period_end);

ALTER TABLE pay_run ADD CONSTRAINT fk_pay_run_journal
    FOREIGN KEY(journal_entry_id) REFERENCES journal_entries(id);

ALTER TABLE pay_run ADD CONSTRAINT fk_pay_run_payment_journal
    FOREIGN KEY(payment_journal_entry_id) REFERENCES journal_entries(id);

CREATE TABLE IF NOT EXISTS pay_payslip(
    id UUID NOT NULL,
    pay_run_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    contract_id UUID NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'draft',
    basic DECIMAL(19, 4) NOT NULL DEFAULT 0,
    allowances DECIMAL(19, 4) NOT NULL DEFAULT 0,
    deductions DECIMAL(19, 4) NOT NULL DEFAULT 0,
    net DECIMAL(19, 4) NOT NULL DEFAULT 0,
    worked_days DECIMAL(8, 2) NOT NULL DEFAULT 0,
    absence_days DECIMAL(8, 2) NOT NULL DEFAULT 0,
    journal_entry_id UUID,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_payslip_run ON pay_payslip(pay_run_id);
CREATE INDEX ix_pay_payslip_employee ON pay_payslip(employee_id);

ALTER TABLE pay_payslip ADD CONSTRAINT fk_pay_payslip_run
    FOREIGN KEY(pay_run_id) REFERENCES pay_run(id) ON DELETE CASCADE;

ALTER TABLE pay_payslip ADD CONSTRAINT fk_pay_payslip_employee
    FOREIGN KEY(employee_id) REFERENCES hr_employee(id);

ALTER TABLE pay_payslip ADD CONSTRAINT fk_pay_payslip_contract
    FOREIGN KEY(contract_id) REFERENCES pay_contract(id);

CREATE TABLE IF NOT EXISTS pay_payslip_line(
    id UUID NOT NULL,
    payslip_id UUID NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    account_id UUID,
    PRIMARY KEY (id)
);

CREATE INDEX ix_pay_payslip_line_payslip ON pay_payslip_line(payslip_id);

ALTER TABLE pay_payslip_line ADD CONSTRAINT fk_pay_payslip_line_payslip
    FOREIGN KEY(payslip_id) REFERENCES pay_payslip(id) ON DELETE CASCADE;

ALTER TABLE pay_payslip_line ADD CONSTRAINT fk_pay_payslip_line_account
    FOREIGN KEY(account_id) REFERENCES accounts(id);
