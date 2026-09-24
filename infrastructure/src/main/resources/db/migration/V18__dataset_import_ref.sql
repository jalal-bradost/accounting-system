CREATE TABLE dataset_import_ref (
    company_id UUID NOT NULL,
    ref_type VARCHAR(32) NOT NULL,
    code VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    PRIMARY KEY (company_id, ref_type, code)
);

CREATE INDEX ix_dataset_import_ref_entity ON dataset_import_ref (entity_id);
