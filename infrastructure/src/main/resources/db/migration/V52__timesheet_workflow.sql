-- Timesheet workflow: cost snapshot on entries (TSH-07), running timers (TSH-04), approval and rounding settings (TSH-05).

ALTER TABLE tsh_entry ADD COLUMN cost_rate DECIMAL(19, 4);
ALTER TABLE tsh_entry ADD COLUMN cost_currency VARCHAR(8);
ALTER TABLE tsh_entry ADD COLUMN cost_amount DECIMAL(19, 4);
ALTER TABLE tsh_entry ADD COLUMN cost_missing BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_tsh_entry_record ON tsh_entry(record_model, record_id);
CREATE INDEX ix_tsh_entry_cost_missing ON tsh_entry(company_id, cost_missing);

ALTER TABLE tsh_company_settings ADD COLUMN managers_may_self_approve BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tsh_company_settings ADD COLUMN auto_lock_after_days INTEGER;
ALTER TABLE tsh_company_settings ADD COLUMN rounding_step_minutes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tsh_company_settings ADD COLUMN rounding_mode VARCHAR(16) NOT NULL DEFAULT 'NEAREST';

CREATE INDEX ix_tsh_task_record ON tsh_task(company_id, record_model, record_id);
CREATE INDEX ix_tsh_week_start ON tsh_week(company_id, week_start);

-- One running timer per employee (BR-TSH-08); the unique index enforces it even under a double click.
CREATE TABLE IF NOT EXISTS tsh_timer(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID,
    description VARCHAR(2000),
    started_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_tsh_timer_employee ON tsh_timer(employee_id);

ALTER TABLE tsh_timer ADD CONSTRAINT fk_tsh_timer_project FOREIGN KEY(project_id) REFERENCES tsh_project(id);
