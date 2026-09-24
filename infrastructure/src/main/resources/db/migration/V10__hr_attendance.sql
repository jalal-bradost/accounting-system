-- Flyway: HR attendances

CREATE TABLE IF NOT EXISTS hr_attendance(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    check_in TIMESTAMP NOT NULL,
    check_out TIMESTAMP,
    check_in_mode VARCHAR(32) NOT NULL DEFAULT 'manual',
    check_out_mode VARCHAR(32),
    extra_hours_minutes INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_hr_attendance_company ON hr_attendance(company_id);

CREATE INDEX ix_hr_attendance_employee ON hr_attendance(employee_id);

CREATE INDEX ix_hr_attendance_check_in ON hr_attendance(company_id, check_in);

ALTER TABLE hr_attendance ADD CONSTRAINT fk_hr_attendance_employee
    FOREIGN KEY(employee_id) REFERENCES hr_employee(id);
