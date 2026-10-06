package com.bradox.erp.sales.service.domain.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates a return picking from the latest DONE delivery.
 * Optional per-move quantities (keyed by stock move id) for partial returns.
 * {@code toRefund} chooses refund (credit note) or replacement when the return is validated.
 */
public class CreateSalesReturnCommand {

    /**
     * When true (default) the customer is refunded: on validation the ordered quantity drops and the
     * invoiced part is credited and posted. When false the goods are replaced (re-delivered).
     */
    private boolean toRefund = true;
    /** Optional return qty per original delivery move id. Empty = full return. */
    private Map<UUID, BigDecimal> moveQuantities = new HashMap<>();

    public boolean isToRefund() { return toRefund; }
    public void setToRefund(boolean toRefund) { this.toRefund = toRefund; }
    public Map<UUID, BigDecimal> getMoveQuantities() { return moveQuantities; }
    public void setMoveQuantities(Map<UUID, BigDecimal> moveQuantities) {
        this.moveQuantities = moveQuantities != null ? moveQuantities : new HashMap<>();
    }
}
