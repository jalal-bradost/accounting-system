-- Replace unused fee revenue accounts with Purchase Discount and Gift Expense.
-- Ensure Sales Discount exists. Track gift lines on customer invoices.

UPDATE accounts
SET name = 'Purchase Discount', type = 'OTHER_INCOME'
WHERE code = '430007'
  AND name IN ('Auction Fee Revenue', 'Auction fee revenue');

UPDATE accounts
SET name = 'Gift Expense', type = 'EXPENSES'
WHERE code = '430008'
  AND name IN ('Delivery Fee Revenue', 'Delivery fee revenue');

INSERT INTO accounts (id, company_id, code, name, type, active)
SELECT UUID(), c.id, '430006', 'Sales Discount', 'EXPENSES', true
FROM platform_company c
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.company_id = c.id AND a.code = '430006'
);

INSERT INTO accounts (id, company_id, code, name, type, active)
SELECT UUID(), c.id, '430007', 'Purchase Discount', 'OTHER_INCOME', true
FROM platform_company c
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.company_id = c.id AND a.code = '430007'
);

INSERT INTO accounts (id, company_id, code, name, type, active)
SELECT UUID(), c.id, '430008', 'Gift Expense', 'EXPENSES', true
FROM platform_company c
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.company_id = c.id AND a.code = '430008'
);

ALTER TABLE acc_customer_invoice_line
    ADD COLUMN is_gift BOOLEAN NOT NULL DEFAULT FALSE;

-- Gift lines on sales orders (free goods expensed to Gift Expense)
ALTER TABLE sal_sales_order_line
    ADD COLUMN is_gift BOOLEAN NOT NULL DEFAULT FALSE;
