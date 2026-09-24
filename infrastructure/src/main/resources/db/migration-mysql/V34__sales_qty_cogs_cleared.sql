-- Track Anglo-Saxon Stock Output → COGS clearing per sales order line.
-- target cleared = LEAST(qty_delivered, qty_invoiced); reconcile posts the delta.

ALTER TABLE sal_sales_order_line
    ADD COLUMN qty_cogs_cleared DECIMAL(19, 4) NOT NULL DEFAULT 0;

UPDATE sal_sales_order_line
SET qty_cogs_cleared = LEAST(COALESCE(qty_delivered, 0), COALESCE(qty_invoiced, 0))
WHERE qty_cogs_cleared = 0
  AND (COALESCE(qty_delivered, 0) > 0 OR COALESCE(qty_invoiced, 0) > 0);
