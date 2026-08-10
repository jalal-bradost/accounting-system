-- Vendor bills can be normal bills or credit notes (purchase returns / AP credits).
ALTER TABLE pur_vendor_bill
    ADD COLUMN IF NOT EXISTS move_type VARCHAR(32) NOT NULL DEFAULT 'BILL',
    ADD COLUMN IF NOT EXISTS reversed_bill_id UUID NULL;

CREATE INDEX IF NOT EXISTS ix_pur_vb_move_type ON pur_vendor_bill (company_id, move_type);
