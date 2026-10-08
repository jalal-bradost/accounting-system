-- Accounting: minimal analytic dimension on journal lines (TSH-10). Not a full analytic-accounts feature.
ALTER TABLE journal_items ADD COLUMN analytic_model VARCHAR(64);
ALTER TABLE journal_items ADD COLUMN analytic_id UUID;
CREATE INDEX ix_journal_items_analytic ON journal_items(analytic_model, analytic_id);

-- Timesheet billing (TSH-06)
ALTER TABLE tsh_project ADD COLUMN default_sale_line_id UUID;

CREATE TABLE IF NOT EXISTS tsh_project_rate(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    project_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    sale_line_id UUID NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_tsh_project_rate ON tsh_project_rate(project_id, employee_id);
ALTER TABLE tsh_project_rate ADD CONSTRAINT fk_tsh_project_rate_project FOREIGN KEY(project_id) REFERENCES tsh_project(id);

CREATE INDEX ix_tsh_entry_sale_line ON tsh_entry(sale_line_id);

-- Timesheet ledger posting (TSH-10)
ALTER TABLE tsh_company_settings ADD COLUMN ledger_posting_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tsh_company_settings ADD COLUMN default_cost_account_id UUID;
ALTER TABLE tsh_company_settings ADD COLUMN labor_applied_account_id UUID;
ALTER TABLE tsh_company_settings ADD COLUMN journal_code VARCHAR(10) NOT NULL DEFAULT 'TSH';

ALTER TABLE tsh_project ADD COLUMN cost_account_id UUID;

-- One row per approval of an employee-week. REVERSED rows stay for history; a reopen reverses, a re-approval adds version + 1.
CREATE TABLE IF NOT EXISTS tsh_week_posting(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    week_id UUID NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    journal_entry_id UUID,
    reversal_entry_id UUID,
    entry_date DATE,
    late_posted BOOLEAN NOT NULL DEFAULT FALSE,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    attempts INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    posted_at TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_tsh_week_posting_version ON tsh_week_posting(week_id, version);
CREATE INDEX ix_tsh_week_posting_status ON tsh_week_posting(company_id, status);
ALTER TABLE tsh_week_posting ADD CONSTRAINT fk_tsh_week_posting_week FOREIGN KEY(week_id) REFERENCES tsh_week(id);

CREATE TABLE IF NOT EXISTS tsh_week_posting_line(
    id UUID NOT NULL,
    posting_id UUID NOT NULL,
    project_id UUID NOT NULL,
    debit_account_id UUID NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    minutes INTEGER NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_tsh_week_posting_line_posting ON tsh_week_posting_line(posting_id);
ALTER TABLE tsh_week_posting_line ADD CONSTRAINT fk_tsh_wpl_posting FOREIGN KEY(posting_id) REFERENCES tsh_week_posting(id);
