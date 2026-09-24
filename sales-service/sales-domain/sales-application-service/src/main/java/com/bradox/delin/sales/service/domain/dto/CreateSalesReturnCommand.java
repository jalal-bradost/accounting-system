package com.bradox.delin.sales.service.domain.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates a return picking from the latest DONE delivery.
 * Optional per-move quantities (keyed by stock move id) for partial returns.
 * {@code toRefund} controls whether a customer credit note is proposed after validate.
 */
public class CreateSalesReturnCommand {

    /** When true (default), auto-create a draft credit note after the return is validated. */
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
