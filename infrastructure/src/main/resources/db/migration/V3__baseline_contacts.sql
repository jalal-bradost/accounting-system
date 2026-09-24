-- Flyway baseline: contacts

CREATE TABLE IF NOT EXISTS contacts_partner(
    active BOOLEAN NOT NULL,
    credit_limit NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3),
    is_customer BOOLEAN NOT NULL,
    is_vendor BOOLEAN NOT NULL,
    archived_at TIMESTAMP,
    language VARCHAR(10),
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    parent_id UUID,
    payable_account_id UUID,
    payment_terms_id UUID,
    receivable_account_id UUID,
    phone VARCHAR(50),
    image_content_type VARCHAR(100),
    tax_id VARCHAR(100),
    image_url VARCHAR(512),
    archived_by VARCHAR(255),
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    legal_name VARCHAR(255),
    website VARCHAR(255),
    kind VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS contacts_partner_address(
    default_for_type BOOLEAN NOT NULL,
    id UUID NOT NULL,
    partner_id UUID NOT NULL,
    postal_code VARCHAR(20),
    city VARCHAR(100),
    country VARCHAR(100),
    state VARCHAR(100),
    street1 VARCHAR(255),
    street2 VARCHAR(255),
    type VARCHAR(64) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS contacts_partner_bank_account(
    id UUID NOT NULL,
    partner_id UUID NOT NULL,
    swift VARCHAR(20),
    iban VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL
,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS contacts_payment_terms(
    active BOOLEAN NOT NULL,
    days_net INTEGER NOT NULL,
    discount_days INTEGER NOT NULL,
    discount_percent NUMERIC(5, 2) NOT NULL,
    archived_at TIMESTAMP,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    archived_by VARCHAR(255)
,
    PRIMARY KEY (id)
);

CREATE INDEX ix_contacts_partner_company ON contacts_partner(company_id, active);

CREATE INDEX ix_contacts_partner_company_name ON contacts_partner(company_id, display_name);

ALTER TABLE contacts_payment_terms ADD CONSTRAINT uk_contacts_payment_terms_company_id_name UNIQUE (company_id, name);

ALTER TABLE contacts_partner_address ADD CONSTRAINT fkfmrd7n56h0kpwstt19guqbhqh FOREIGN KEY(partner_id) REFERENCES contacts_partner(id);

ALTER TABLE contacts_partner_bank_account ADD CONSTRAINT fk58k2v3me3j4ko5adjlrilfcnb FOREIGN KEY(partner_id) REFERENCES contacts_partner(id);
