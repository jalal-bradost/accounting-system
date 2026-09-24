ALTER TABLE pur_vendor_bill_line
    ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(19, 4) NOT NULL DEFAULT 0;

-- Backfill combined line + order discount from the source purchase order.
-- Correlated subqueries (H2 PostgreSQL mode does not support UPDATE ... FROM ... JOIN).
UPDATE pur_vendor_bill_line vbl
SET discount_percent = LEAST(
        100,
        GREATEST(
            0,
            ROUND(
                (1 - (1 - COALESCE((
                        SELECT pol.discount_percent
                        FROM pur_purchase_order_line pol
                        WHERE pol.id = vbl.purchase_order_line_id
                    ), 0) / 100.0)
                    * (1 - COALESCE((
                        SELECT po.order_discount_percent
                        FROM pur_vendor_bill vb
                        JOIN pur_purchase_order po ON po.id = vb.purchase_order_id
                        WHERE vb.id = vbl.vendor_bill_id
                    ), 0) / 100.0)) * 100,
                8
            )
        )
    )
WHERE vbl.purchase_order_line_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM pur_vendor_bill vb
      WHERE vb.id = vbl.vendor_bill_id
        AND vb.purchase_order_id IS NOT NULL
  );
