-- Timesheet module (TSH-01 projects and tasks, TSH-02 time entries, TSH-03 weekly grid).
-- Durations are whole minutes (D2). work_date is a calendar date in the company timezone (D3).

CREATE TABLE IF NOT EXISTS tsh_company_settings(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    time_format VARCHAR(16) NOT NULL DEFAULT 'HH_MM',
    week_start_day VARCHAR(16) NOT NULL DEFAULT 'SATURDAY',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Baghdad',
    approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    allow_future_days INTEGER NOT NULL DEFAULT 0,
    minutes_per_day INTEGER NOT NULL DEFAULT 480,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_tsh_company_settings_company ON tsh_company_settings(company_id);

CREATE TABLE IF NOT EXISTS tsh_project(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    code_normalized VARCHAR(50),
    partner_id UUID,
    manager_employee_id UUID,
    allow_timesheets BOOLEAN NOT NULL DEFAULT TRUE,
    billing_mode VARCHAR(16) NOT NULL DEFAULT 'HOURLY',
    billable_default BOOLEAN NOT NULL DEFAULT FALSE,
    allocated_minutes INTEGER,
    linked_model VARCHAR(100),
    system_key VARCHAR(50),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    color VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX ix_tsh_project_company ON tsh_project(company_id, status);
CREATE UNIQUE INDEX uk_tsh_project_code ON tsh_project(company_id, code_normalized);
CREATE UNIQUE INDEX uk_tsh_project_system_key ON tsh_project(company_id, system_key);

CREATE TABLE IF NOT EXISTS tsh_task(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(4000),
    status VARCHAR(16) NOT NULL DEFAULT 'TODO',
    status_changed_at TIMESTAMP,
    allocated_minutes INTEGER,
    deadline DATE,
    sale_line_id UUID,
    record_model VARCHAR(100),
    record_id UUID,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX ix_tsh_task_project ON tsh_task(company_id, project_id);

ALTER TABLE tsh_task ADD CONSTRAINT fk_tsh_task_project
    FOREIGN KEY(project_id) REFERENCES tsh_project(id);

CREATE TABLE IF NOT EXISTS tsh_task_assignee(
    task_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    PRIMARY KEY (task_id, employee_id)
);

ALTER TABLE tsh_task_assignee ADD CONSTRAINT fk_tsh_task_assignee_task
    FOREIGN KEY(task_id) REFERENCES tsh_task(id) ON DELETE CASCADE;

-- One row per employee-week. status moves DRAFT -> SUBMITTED -> APPROVED/REFUSED (TSH-05).
CREATE TABLE IF NOT EXISTS tsh_week(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    week_start DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMP,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP,
    refused_reason VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_tsh_week_employee_start ON tsh_week(employee_id, week_start);
CREATE INDEX ix_tsh_week_company_status ON tsh_week(company_id, status);

CREATE TABLE IF NOT EXISTS tsh_entry(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    work_date DATE NOT NULL,
    minutes INTEGER NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID,
    description VARCHAR(2000),
    billable BOOLEAN NOT NULL DEFAULT FALSE,
    sale_line_id UUID,
    week_id UUID NOT NULL,
    record_model VARCHAR(100),
    record_id UUID,
    source VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_tsh_entry_minutes CHECK (minutes BETWEEN 1 AND 1440)
);

CREATE INDEX ix_tsh_entry_employee_date ON tsh_entry(company_id, employee_id, work_date);
CREATE INDEX ix_tsh_entry_project_date ON tsh_entry(company_id, project_id, work_date);
CREATE INDEX ix_tsh_entry_week ON tsh_entry(week_id);
CREATE INDEX ix_tsh_entry_task ON tsh_entry(task_id);

ALTER TABLE tsh_entry ADD CONSTRAINT fk_tsh_entry_project
    FOREIGN KEY(project_id) REFERENCES tsh_project(id);
ALTER TABLE tsh_entry ADD CONSTRAINT fk_tsh_entry_task
    FOREIGN KEY(task_id) REFERENCES tsh_task(id);
ALTER TABLE tsh_entry ADD CONSTRAINT fk_tsh_entry_week
    FOREIGN KEY(week_id) REFERENCES tsh_week(id);

-- Rows the employee added to their grid ("Add a line") that may have no entries yet (TSH-03 #4).
CREATE TABLE IF NOT EXISTS tsh_grid_line(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID,
    line_key VARCHAR(80) NOT NULL,
    PRIMARY KEY (id)
);

-- line_key = projectId + ':' + (taskId or '-'), keeps the pair unique even when task_id is NULL.
CREATE UNIQUE INDEX uk_tsh_grid_line ON tsh_grid_line(employee_id, line_key);

ALTER TABLE tsh_grid_line ADD CONSTRAINT fk_tsh_grid_line_project
    FOREIGN KEY(project_id) REFERENCES tsh_project(id);
