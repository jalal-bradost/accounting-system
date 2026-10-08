-- Timesheet reminders (TSH-05 #8): who is reminded, on which weekday, and a log so a run never reminds twice.
ALTER TABLE tsh_company_settings ADD COLUMN reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tsh_company_settings ADD COLUMN reminder_weekday VARCHAR(16) NOT NULL DEFAULT 'SUNDAY';

CREATE TABLE IF NOT EXISTS tsh_reminder_log(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    run_week DATE NOT NULL,
    kind VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- "Once per week per employee": one EMPLOYEE row and one MANAGER row per run week.
CREATE UNIQUE INDEX uk_tsh_reminder_log ON tsh_reminder_log(employee_id, run_week, kind);
