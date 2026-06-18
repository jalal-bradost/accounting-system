package com.jalaldeveloper.accountingsystem.sales.domain.core.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class PricelistItem {

    private UUID id;
    private int sequence;
    private UUID productId;
    private BigDecimal minQuantity;
    private BigDecimal fixedPrice;
    private BigDecimal percentDiscount;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public BigDecimal getMinQuantity() { return minQuantity; }
    public void setMinQuantity(BigDecimal minQuantity) { this.minQuantity = minQuantity; }
    public BigDecimal getFixedPrice() { return fixedPrice; }
    public void setFixedPrice(BigDecimal fixedPrice) { this.fixedPrice = fixedPrice; }
    public BigDecimal getPercentDiscount() { return percentDiscount; }
    public void setPercentDiscount(BigDecimal percentDiscount) { this.percentDiscount = percentDiscount; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
