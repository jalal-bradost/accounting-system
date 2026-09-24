-- Platform chatter tables (record followers & attachments)

CREATE TABLE IF NOT EXISTS platform_record_attachment(
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    record_id UUID NOT NULL,
    content_type VARCHAR(127),
    file_name VARCHAR(255) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    public_url VARCHAR(512) NOT NULL,
    uploaded_by VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX ix_record_attachment_target ON platform_record_attachment(company_id, model_name, record_id);

CREATE TABLE IF NOT EXISTS platform_record_follower(
    notify_on_post BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    company_id UUID NOT NULL,
    id UUID NOT NULL,
    partner_id UUID NOT NULL,
    record_id UUID NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    added_by VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX ix_record_follower_target ON platform_record_follower(company_id, model_name, record_id);

ALTER TABLE platform_record_follower ADD CONSTRAINT uk_record_follower_partner UNIQUE (company_id, model_name, record_id, partner_id);
