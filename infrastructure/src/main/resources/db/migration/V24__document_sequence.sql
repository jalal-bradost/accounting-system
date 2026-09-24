CREATE TABLE IF NOT EXISTS plat_document_sequence (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    doc_type VARCHAR(32) NOT NULL,
    seq_year INT NOT NULL,
    last_value BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_plat_doc_seq UNIQUE (company_id, doc_type, seq_year)
);
