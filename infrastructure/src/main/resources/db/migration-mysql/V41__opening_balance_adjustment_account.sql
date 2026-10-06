-- Opening Balance Adjustment equity account for post-go-live partner corrections.
INSERT INTO accounts (id, company_id, code, name, type, active)
SELECT UUID(), c.id, '430025', 'Opening Balance Adjustment', 'EQUITY', true
FROM platform_company c
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.company_id = c.id AND a.code = '430025'
);
