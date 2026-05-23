package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class ClosePosSessionCommand {
    @NotNull
    @PositiveOrZero
    private BigDecimal closingCash;

    public BigDecimal getClosingCash() { return closingCash; }
    public void setClosingCash(BigDecimal closingCash) { this.closingCash = closingCash; }
}
