-- Sign simplified: a template is a PDF with boxes (signature, name, date, text), signed on screen.
-- Each signing stores a signed copy in Documents and one sign_document row.
-- The phase 1 request workflow (roles, signers, links, reminders, event log) is no longer used.

DROP TABLE IF EXISTS sign_event;
DROP TABLE IF EXISTS sign_request_field;
DROP TABLE IF EXISTS sign_signer_item;
DROP TABLE IF EXISTS sign_request;
DROP TABLE IF EXISTS sign_counter;
DROP TABLE IF EXISTS sign_template_field;
DROP TABLE IF EXISTS sign_template_role;
DROP TABLE IF EXISTS sign_template;

CREATE TABLE IF NOT EXISTS sign_template(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    document_id UUID NOT NULL,
    document_sha256 VARCHAR(64) NOT NULL,
    page_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_template_company ON sign_template(company_id);

CREATE TABLE IF NOT EXISTS sign_template_field(
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    sequence INTEGER NOT NULL,
    page INTEGER NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    field_type VARCHAR(16) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_template_field_template ON sign_template_field(template_id);

CREATE TABLE IF NOT EXISTS sign_document(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    template_id UUID NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    signer_name VARCHAR(255) NOT NULL,
    signed_at TIMESTAMP NOT NULL,
    signed_by VARCHAR(255),
    document_id UUID NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX ix_sign_document_template ON sign_document(company_id, template_id);
