package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Dated rate line for a {@link CompanyCurrencyEntity}. Many lines per currency,
 * unique on (currency_id, effective_date). The "current" rate at any point in
 * time is the line whose {@code effective_date} is the latest one ≤ that date,
 * mirroring Odoo's res.currency.rate model.
 *
 * <p>Existing lines must be preserved: posted accounting transactions resolve
 * their FX rate by transaction date, so historical rates must remain queryable.
 */
@Entity
@Table(
        name = "currency_rates",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"currency_id", "effective_date"}))
public class CurrencyRateEntity {

    @Id
    private UUID id;

    @Column(name = "currency_id", nullable = false)
    private UUID currencyId;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    public CurrencyRateEntity() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(UUID currencyId) {
        this.currencyId = currencyId;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
