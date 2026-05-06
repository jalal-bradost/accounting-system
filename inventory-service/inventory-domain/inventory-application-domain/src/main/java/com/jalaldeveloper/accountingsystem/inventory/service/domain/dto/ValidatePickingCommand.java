package com.jalaldeveloper.accountingsystem.inventory.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Optional per-move picked qty overrides at validation time. If omitted, every move is
 * validated for its full {@code demandQuantity}. Picked &lt; demand triggers a backorder.
 */
public class ValidatePickingCommand {

    private List<MovePicked> picks;
    private boolean createBackorder = true;

    public List<MovePicked> getPicks() { return picks; }
    public void setPicks(List<MovePicked> v) { this.picks = v; }
    public boolean isCreateBackorder() { return createBackorder; }
    public void setCreateBackorder(boolean v) { this.createBackorder = v; }

    public static class MovePicked {
        @NotNull private UUID moveId;
        @NotNull @PositiveOrZero private BigDecimal pickedQuantity;

        public UUID getMoveId() { return moveId; }
        public void setMoveId(UUID v) { this.moveId = v; }
        public BigDecimal getPickedQuantity() { return pickedQuantity; }
        public void setPickedQuantity(BigDecimal v) { this.pickedQuantity = v; }
    }
}
