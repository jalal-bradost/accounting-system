-- Flyway baseline: hr

CREATE TABLE IF NOT EXISTS hr_department(
    active BOOLEAN NOT NULL,
    color_index INTEGER NOT NULL DEFAULT 0,
    archived_at TIMESTAMP,
    company_id CHAR(36) NOT NULL,
    id CHAR(36) NOT NULL,
    parent_id CHAR(36),
    manager_id CHAR(36),
    name VARCHAR(255) NOT NULL,
    archived_by VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS hr_employee(
    active BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    hire_date DATE,
    company_id CHAR(36) NOT NULL,
    department_id CHAR(36),
    id CHAR(36) NOT NULL,
    manager_id CHAR(36),
    work_phone VARCHAR(50),
    mobile_phone VARCHAR(50),
    work_postal_code VARCHAR(20),
    image_content_type VARCHAR(100),
    job_title VARCHAR(255),
    work_city VARCHAR(100),
    work_country VARCHAR(100),
    work_state VARCHAR(100),
    image_url VARCHAR(512),
    archived_by VARCHAR(255),
    display_name VARCHAR(255) NOT NULL,
    work_email VARCHAR(255),
    work_location VARCHAR(255),
    work_street VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE INDEX ix_hr_department_company ON hr_department(company_id);

CREATE INDEX ix_hr_employee_company ON hr_employee(company_id);

ALTER TABLE hr_employee ADD CONSTRAINT fk_hr_employee_department FOREIGN KEY(department_id) REFERENCES hr_department(id);

ALTER TABLE hr_employee ADD CONSTRAINT fk_hr_employee_manager FOREIGN KEY(manager_id) REFERENCES hr_employee(id);

ALTER TABLE hr_department ADD CONSTRAINT fk_hr_department_manager FOREIGN KEY(manager_id) REFERENCES hr_employee(id);

ALTER TABLE hr_department ADD CONSTRAINT fk_hr_department_parent FOREIGN KEY(parent_id) REFERENCES hr_department(id);
