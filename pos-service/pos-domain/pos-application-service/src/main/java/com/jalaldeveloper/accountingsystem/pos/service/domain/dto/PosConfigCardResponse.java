package com.jalaldeveloper.accountingsystem.pos.service.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PosConfigCardResponse {
    private UUID id;
    private String name;
    private String currencyCode;
    private boolean active;
    private UUID openSessionId;
    private String openSessionState;
    private Instant sessionOpenedAt;
    private BigDecimal openingCash;
    private BigDecimal sessionSalesTotal;
    private long sessionOrderCount;
    private BigDecimal lastClosingCash;
    private Instant lastClosedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public UUID getOpenSessionId() { return openSessionId; }
    public void setOpenSessionId(UUID openSessionId) { this.openSessionId = openSessionId; }
    public String getOpenSessionState() { return openSessionState; }
    public void setOpenSessionState(String openSessionState) { this.openSessionState = openSessionState; }
    public Instant getSessionOpenedAt() { return sessionOpenedAt; }
    public void setSessionOpenedAt(Instant sessionOpenedAt) { this.sessionOpenedAt = sessionOpenedAt; }
    public BigDecimal getOpeningCash() { return openingCash; }
    public void setOpeningCash(BigDecimal openingCash) { this.openingCash = openingCash; }
    public BigDecimal getSessionSalesTotal() { return sessionSalesTotal; }
    public void setSessionSalesTotal(BigDecimal sessionSalesTotal) { this.sessionSalesTotal = sessionSalesTotal; }
    public long getSessionOrderCount() { return sessionOrderCount; }
    public void setSessionOrderCount(long sessionOrderCount) { this.sessionOrderCount = sessionOrderCount; }
    public BigDecimal getLastClosingCash() { return lastClosingCash; }
    public void setLastClosingCash(BigDecimal lastClosingCash) { this.lastClosingCash = lastClosingCash; }
    public Instant getLastClosedAt() { return lastClosedAt; }
    public void setLastClosedAt(Instant lastClosedAt) { this.lastClosedAt = lastClosedAt; }
}
