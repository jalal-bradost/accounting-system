-- Vendor bills can be normal bills or credit notes (purchase returns / AP credits).
ALTER TABLE pur_vendor_bill ADD COLUMN move_type VARCHAR(32) NOT NULL DEFAULT 'BILL';
ALTER TABLE pur_vendor_bill ADD COLUMN reversed_bill_id CHAR(36) NULL;

CREATE INDEX ix_pur_vb_move_type ON pur_vendor_bill (company_id, move_type);