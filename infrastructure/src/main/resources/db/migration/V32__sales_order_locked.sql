-- Sales order lock (mirrors purchase order locked)

ALTER TABLE sal_sales_order ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT FALSE;
