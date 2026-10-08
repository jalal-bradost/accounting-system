-- Sign (e-signature) phase 1: templates, requests, signers, fields, and the hash-chained event log.
-- Signature images are stored as base64 text so the same script runs on H2, PostgreSQL and MySQL.

CREATE TABLE IF NOT EXISTS sign_template(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    document_id UUID NOT NULL,
    document_version_id UUID,
    document_sha256 VARCHAR(64) NOT NULL,
    page_count INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    default_validity_days INTEGER NOT NULL,
    default_message VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_template_company ON sign_template(company_id);

CREATE TABLE IF NOT EXISTS sign_template_role(
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    sequence INTEGER NOT NULL,
    approver_only BOOLEAN NOT NULL,
    default_partner_id UUID,
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_template_role_template ON sign_template_role(template_id);

CREATE TABLE IF NOT EXISTS sign_template_field(
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    role_id UUID NOT NULL,
    page INTEGER NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    field_type VARCHAR(16) NOT NULL,
    required BOOLEAN NOT NULL,
    label VARCHAR(255),
    placeholder VARCHAR(255),
    auto_fill VARCHAR(16) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_template_field_template ON sign_template_field(template_id);

CREATE TABLE IF NOT EXISTS sign_request(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    reference VARCHAR(50) NOT NULL,
    template_id UUID,
    source_document_id UUID NOT NULL,
    source_version_id UUID,
    source_sha256 VARCHAR(64) NOT NULL,
    page_count INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    signing_order VARCHAR(16) NOT NULL,
    message VARCHAR(2000),
    expires_at TIMESTAMP,
    reminder_every_days INTEGER,
    reminder_prompts INTEGER NOT NULL,
    last_reminder_at TIMESTAMP,
    record_model VARCHAR(100),
    record_id UUID,
    final_document_id UUID,
    final_sha256 VARCHAR(64),
    sent_at TIMESTAMP,
    completed_at TIMESTAMP,
    canceled_at TIMESTAMP,
    cancel_reason VARCHAR(1000),
    needs_attention BOOLEAN NOT NULL,
    attention_reason VARCHAR(1000),
    build_attempts INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    created_by_user_id UUID,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sign_request_reference ON sign_request(company_id, reference);
CREATE INDEX ix_sign_request_company_status ON sign_request(company_id, status);
CREATE INDEX ix_sign_request_record ON sign_request(company_id, record_model, record_id);
CREATE INDEX ix_sign_request_final_sha ON sign_request(final_sha256);

CREATE TABLE IF NOT EXISTS sign_signer_item(
    id UUID NOT NULL,
    request_id UUID NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    sequence INTEGER NOT NULL,
    approver_only BOOLEAN NOT NULL,
    partner_id UUID,
    user_id UUID,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    channel VARCHAR(16) NOT NULL,
    token_hash VARCHAR(64),
    token_expires_at TIMESTAMP,
    status VARCHAR(16) NOT NULL,
    viewed_at TIMESTAMP,
    signed_at TIMESTAMP,
    refused_at TIMESTAMP,
    refuse_reason VARCHAR(1000),
    signed_ip VARCHAR(64),
    signed_user_agent VARCHAR(500),
    submission_digest VARCHAR(64),
    link_issued BOOLEAN NOT NULL,
    operator_user_id UUID,
    id_checked BOOLEAN NOT NULL,
    id_note VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_signer_request ON sign_signer_item(request_id);
CREATE UNIQUE INDEX uk_sign_signer_token ON sign_signer_item(token_hash);
CREATE INDEX ix_sign_signer_user ON sign_signer_item(user_id);

CREATE TABLE IF NOT EXISTS sign_request_field(
    id UUID NOT NULL,
    request_id UUID NOT NULL,
    signer_item_id UUID NOT NULL,
    page INTEGER NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    field_type VARCHAR(16) NOT NULL,
    required BOOLEAN NOT NULL,
    label VARCHAR(255),
    placeholder VARCHAR(255),
    auto_fill VARCHAR(16) NOT NULL,
    value_text VARCHAR(1000),
    value_bool BOOLEAN,
    value_image TEXT,
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_field_request ON sign_request_field(request_id);

-- Append only: nothing in the code updates or deletes a row here.
CREATE TABLE IF NOT EXISTS sign_event(
    id UUID NOT NULL,
    request_id UUID NOT NULL,
    signer_item_id UUID,
    event_type VARCHAR(24) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    ip VARCHAR(64),
    user_agent VARCHAR(500),
    details VARCHAR(2000),
    prev_hash VARCHAR(64) NOT NULL,
    event_hash VARCHAR(64) NOT NULL,
    seq BIGINT NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_sign_event_seq ON sign_event(request_id, seq);

CREATE TABLE IF NOT EXISTS sign_counter(
    company_id UUID NOT NULL,
    counter_year INTEGER NOT NULL,
    last_value INTEGER NOT NULL,
    PRIMARY KEY (company_id, counter_year)
);
