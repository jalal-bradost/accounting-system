package com.bradox.erp.inventory.dataaccess.adapter;

import com.bradox.erp.inventory.service.domain.ports.output.ProductMovementTotalsPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ProductMovementTotalsAdapter implements ProductMovementTotalsPort {

    /** Validated moves only; deliveries/receipts count up and their returns count down. */
    private static final String SIGNED_QTY =
            "SELECT COALESCE(SUM(CASE WHEN pk.picking_type = :outType THEN m.picked_quantity "
                    + "ELSE -m.picked_quantity END), 0) "
                    + "FROM inv_stock_move m "
                    + "JOIN inv_stock_picking pk ON pk.id = m.picking_id ";

    private static final String VALIDATED =
            "WHERE pk.company_id = :companyId AND m.product_id = :productId "
                    + "AND m.state = 'DONE' AND pk.state = 'DONE' "
                    + "AND pk.picking_type IN ('OUTGOING', 'INCOMING') ";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BigDecimal soldQuantity(UUID companyId, UUID productId) {
        Query q = entityManager.createNativeQuery(SIGNED_QTY
                + "JOIN sal_sales_order_line l ON l.id = m.sales_order_line_id "
                + VALIDATED
                + "AND COALESCE(l.is_gift, FALSE) = FALSE");
        q.setParameter("outType", "OUTGOING");
        return run(q, companyId, productId);
    }

    @Override
    public BigDecimal purchasedQuantity(UUID companyId, UUID productId) {
        Query q = entityManager.createNativeQuery(SIGNED_QTY
                + VALIDATED
                + "AND m.purchase_order_line_id IS NOT NULL");
        q.setParameter("outType", "INCOMING");
        return run(q, companyId, productId);
    }

    private static BigDecimal run(Query q, UUID companyId, UUID productId) {
        q.setParameter("companyId", companyId);
        q.setParameter("productId", productId);
        Object value = q.getSingleResult();
        return value instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(value));
    }
}
