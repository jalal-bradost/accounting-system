ALTER TABLE pur_vendor_bill_line ADD COLUMN discount_percent DECIMAL(19, 4) NOT NULL DEFAULT 0;

UPDATE pur_vendor_bill_line vbl
    INNER JOIN pur_vendor_bill vb ON vbl.vendor_bill_id = vb.id
    INNER JOIN pur_purchase_order po ON po.id = vb.purchase_order_id
    INNER JOIN pur_purchase_order_line pol ON pol.id = vbl.purchase_order_line_id
SET vbl.discount_percent = LEAST(
        100,
        GREATEST(
            0,
            ROUND(
                (1 - (1 - COALESCE(pol.discount_percent, 0) / 100.0)
                    * (1 - COALESCE(po.order_discount_percent, 0) / 100.0)) * 100,
                8
            )
        )
    )
WHERE vb.purchase_order_id IS NOT NULL
  AND vbl.purchase_order_line_id IS NOT NULL;
