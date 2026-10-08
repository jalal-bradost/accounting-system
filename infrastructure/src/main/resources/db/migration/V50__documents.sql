-- Documents module: folders, documents, versions, tags, record links, folder access.

CREATE TABLE IF NOT EXISTS doc_folder(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    parent_id UUID,
    name VARCHAR(255) NOT NULL,
    name_normalized VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL DEFAULT 0,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    inherit_access BOOLEAN NOT NULL DEFAULT TRUE,
    linked_model VARCHAR(100),
    system_key VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX ix_doc_folder_company ON doc_folder(company_id);
CREATE INDEX ix_doc_folder_parent ON doc_folder(company_id, parent_id);

ALTER TABLE doc_folder ADD CONSTRAINT fk_doc_folder_parent
    FOREIGN KEY(parent_id) REFERENCES doc_folder(id);

CREATE TABLE IF NOT EXISTS doc_folder_access(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    folder_id UUID NOT NULL,
    subject_type VARCHAR(16) NOT NULL,
    subject_id UUID NOT NULL,
    level VARCHAR(16) NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_doc_folder_access_subject ON doc_folder_access(folder_id, subject_type, subject_id);
CREATE INDEX ix_doc_folder_access_company ON doc_folder_access(company_id);

ALTER TABLE doc_folder_access ADD CONSTRAINT fk_doc_folder_access_folder
    FOREIGN KEY(folder_id) REFERENCES doc_folder(id);

CREATE TABLE IF NOT EXISTS doc_document(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    folder_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    name_normalized VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(16) NOT NULL,
    source VARCHAR(16) NOT NULL,
    owner_user_id UUID,
    current_version_id UUID,
    version_no INTEGER NOT NULL DEFAULT 0,
    file_name VARCHAR(255),
    file_name_normalized VARCHAR(255),
    content_type VARCHAR(127),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(64),
    trashed_at TIMESTAMP,
    trashed_by VARCHAR(255),
    retention_until DATE,
    retention_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX ix_doc_document_folder ON doc_document(company_id, folder_id, status);
CREATE INDEX ix_doc_document_name ON doc_document(company_id, name_normalized);
CREATE INDEX ix_doc_document_sha ON doc_document(company_id, folder_id, sha256);
CREATE INDEX ix_doc_document_trashed ON doc_document(status, trashed_at);

ALTER TABLE doc_document ADD CONSTRAINT fk_doc_document_folder
    FOREIGN KEY(folder_id) REFERENCES doc_folder(id);

CREATE TABLE IF NOT EXISTS doc_document_version(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    document_id UUID NOT NULL,
    version_no INTEGER NOT NULL,
    storage_key VARCHAR(64) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_by VARCHAR(255),
    uploaded_at TIMESTAMP NOT NULL,
    comment VARCHAR(500),
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_doc_version_no ON doc_document_version(document_id, version_no);
CREATE INDEX ix_doc_version_company ON doc_document_version(company_id);

ALTER TABLE doc_document_version ADD CONSTRAINT fk_doc_version_document
    FOREIGN KEY(document_id) REFERENCES doc_document(id);

CREATE TABLE IF NOT EXISTS doc_tag_facet(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_doc_tag_facet_company ON doc_tag_facet(company_id);

CREATE TABLE IF NOT EXISTS doc_tag(
    id UUID NOT NULL,
    company_id UUID NOT NULL,
    facet_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(20) NOT NULL,
    sequence INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX ix_doc_tag_company ON doc_tag(company_id);

ALTER TABLE doc_tag ADD CONSTRAINT fk_doc_tag_facet
    FOREIGN KEY(facet_id) REFERENCES doc_tag_facet(id);

CREATE TABLE IF NOT EXISTS doc_document_tag(
    document_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (document_id, tag_id)
);

CREATE INDEX ix_doc_document_tag_tag ON doc_document_tag(tag_id);

ALTER TABLE doc_document_tag ADD CONSTRAINT fk_doc_document_tag_document
    FOREIGN KEY(document_id) REFERENCES doc_document(id);
ALTER TABLE doc_document_tag ADD CONSTRAINT fk_doc_document_tag_tag
    FOREIGN KEY(tag_id) REFERENCES doc_tag(id);

CREATE TABLE IF NOT EXISTS doc_record_link(
    document_id UUID NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    PRIMARY KEY (document_id, model_name, record_id)
);

CREATE INDEX ix_doc_record_link_record ON doc_record_link(model_name, record_id);

ALTER TABLE doc_record_link ADD CONSTRAINT fk_doc_record_link_document
    FOREIGN KEY(document_id) REFERENCES doc_document(id);
