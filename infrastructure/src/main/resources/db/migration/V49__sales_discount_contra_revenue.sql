-- Sales Discount is a deduction from revenue (contra-income), not an operating expense.
-- As INCOME it sits in the Profit & Loss revenue section, so revenue there is net sales and
-- matches the dashboards. Net income does not change.
UPDATE accounts
SET type = 'INCOME'
WHERE code = '430006'
  AND name = 'Sales Discount'
  AND type = 'EXPENSES';
