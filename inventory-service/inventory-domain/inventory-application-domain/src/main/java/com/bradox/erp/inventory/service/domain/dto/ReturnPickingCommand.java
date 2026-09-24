package com.bradox.erp.inventory.service.domain.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Optional per-move return quantities keyed by original move id.
 * When empty/null, each DONE move is returned for its full picked quantity.
 * {@code toRefund} controls whether a vendor credit note is proposed after validate (purchase).
 */
public class ReturnPickingCommand {

    private Map<UUID, BigDecimal> moveQuantities = new HashMap<>();
    private boolean toRefund = true;

    public Map<UUID, BigDecimal> getMoveQuantities() { return moveQuantities; }

    public void setMoveQuantities(Map<UUID, BigDecimal> moveQuantities) {
        this.moveQuantities = moveQuantities != null ? moveQuantities : new HashMap<>();
    }

    public boolean isToRefund() { return toRefund; }

    public void setToRefund(boolean toRefund) { this.toRefund = toRefund; }
}
