-- Flyway baseline: platform

CREATE TABLE IF NOT EXISTS platform_activity_message(
    due_date DATE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    record_id UUID NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    assignee_id VARCHAR(255),
    author_id VARCHAR(255),
    subject VARCHAR(255),
    body TEXT,
    kind VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS platform_app_user(
    active BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    archived_by VARCHAR(255),
    display_name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS platform_audit_log(
    occurred_at TIMESTAMP NOT NULL,
    company_id UUID,
    id UUID NOT NULL,
    record_id UUID,
    model_name VARCHAR(100) NOT NULL,
    message VARCHAR(1000),
    user_id VARCHAR(255),
    action VARCHAR(64) NOT NULL,
    changes TEXT
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS platform_company(
    active BOOLEAN NOT NULL,
    country VARCHAR(2),
    default_currency VARCHAR(3),
    fiscal_year_start_month INTEGER,
    period_lock_date DATE,
    archived_at TIMESTAMP,
    id UUID NOT NULL,
    locale VARCHAR(20),
    date_format VARCHAR(30),
    number_format VARCHAR(30),
    postal_code VARCHAR(30),
    phone VARCHAR(50),
    city VARCHAR(100),
    state VARCHAR(100),
    tax_id VARCHAR(100),
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    email VARCHAR(200),
    legal_name VARCHAR(200),
    name VARCHAR(200) NOT NULL,
    website VARCHAR(200),
    logo_url VARCHAR(500),
    archived_by VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS platform_permission(
    id UUID NOT NULL,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS platform_processed_event(
    processed_at TIMESTAMP NOT NULL,
    event_id UUID NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL
,
    PRIMARY KEY (event_id, consumer_name)
);

CREATE TABLE IF NOT EXISTS platform_role(
    active BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    archived_by VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS platform_role_permission(
    permission_id UUID NOT NULL,
    role_id UUID NOT NULL
,
    PRIMARY KEY (permission_id, role_id)
);

CREATE TABLE IF NOT EXISTS platform_user_role(
    role_id UUID NOT NULL,
    user_id UUID NOT NULL
,
    PRIMARY KEY (role_id, user_id)
);

CREATE INDEX ix_activity_company_model_record ON platform_activity_message(company_id, model_name, record_id);

CREATE INDEX ix_activity_assignee_open ON platform_activity_message(assignee_id, completed_at);

CREATE INDEX ix_audit_company_model_record ON platform_audit_log(company_id, model_name, record_id);

CREATE INDEX ix_audit_occurred_at ON platform_audit_log(occurred_at);

ALTER TABLE platform_app_user ADD CONSTRAINT uk_platform_app_user_company_id_email UNIQUE (company_id, email);

ALTER TABLE platform_permission ADD CONSTRAINT uk_platform_permission_code UNIQUE (code);

ALTER TABLE platform_role ADD CONSTRAINT uk_platform_role_company_id_code UNIQUE (company_id, code);

ALTER TABLE platform_app_user ADD CONSTRAINT uk_platform_app_user_company_id_username UNIQUE (company_id, username);
