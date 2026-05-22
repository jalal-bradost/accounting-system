package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Exchange rates relative to the company base currency (see {@link #baseCurrency}).
 * {@code ratePerBase} is "units of this currency per 1 unit of base" (e.g. IQD per 1 USD).
 */
@Entity
@Table(
        name = "company_currencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
public class CompanyCurrencyEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 3)
    private String code;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "last_rate_updated")
    private LocalDate lastRateUpdated;

    @Column(name = "rate_per_base", nullable = false, precision = 19, scale = 6)
    private BigDecimal ratePerBase;

    @Column(name = "base_currency", nullable = false)
    private boolean baseCurrency;

    @Column(nullable = false)
    private boolean active;

    public CompanyCurrencyEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getLastRateUpdated() {
        return lastRateUpdated;
    }

    public void setLastRateUpdated(LocalDate lastRateUpdated) {
        this.lastRateUpdated = lastRateUpdated;
    }

    public BigDecimal getRatePerBase() {
        return ratePerBase;
    }

    public void setRatePerBase(BigDecimal ratePerBase) {
        this.ratePerBase = ratePerBase;
    }

    public boolean isBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(boolean baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
