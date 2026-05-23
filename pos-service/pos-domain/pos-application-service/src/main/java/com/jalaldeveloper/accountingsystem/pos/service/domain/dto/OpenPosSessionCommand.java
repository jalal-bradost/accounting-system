package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public class OpenPosSessionCommand {
    private UUID companyId;
    @NotNull
    private UUID configId;
    @PositiveOrZero
    private BigDecimal openingCash = BigDecimal.ZERO;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getConfigId() { return configId; }
    public void setConfigId(UUID configId) { this.configId = configId; }
    public BigDecimal getOpeningCash() { return openingCash; }
    public void setOpeningCash(BigDecimal openingCash) { this.openingCash = openingCash; }
}
