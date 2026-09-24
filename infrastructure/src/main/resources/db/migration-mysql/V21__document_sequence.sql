CREATE TABLE IF NOT EXISTS plat_document_sequence (
    id CHAR(36) NOT NULL,
    company_id CHAR(36) NOT NULL,
    doc_type VARCHAR(32) NOT NULL,
    seq_year INT NOT NULL,
    last_value BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_plat_doc_seq UNIQUE (company_id, doc_type, seq_year)
);
