-- Purchase Price Variance: the difference between what goods were received at and what they were
-- billed at. Kept off Stock Input (GR/IR) so it clears once receipt and bill agree on quantity.
INSERT INTO accounts (id, company_id, code, name, type, active)
SELECT gen_random_uuid(), c.id, '430026', 'Purchase Price Variance', 'EXPENSES', true
FROM platform_company c
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.company_id = c.id AND a.code = '430026'
);

ALTER TABLE pur_vendor_bill_line
    ADD COLUMN IF NOT EXISTS price_variance DECIMAL(19, 4) NOT NULL DEFAULT 0;
