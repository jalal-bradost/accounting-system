package com.jalaldeveloper.accountingsystem.sales.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sal_pricelist_item", indexes = {
        @Index(name = "ix_sal_pli_pl", columnList = "pricelist_id"),
        @Index(name = "ix_sal_pli_product", columnList = "product_id")
})
public class SalPricelistItemEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pricelist_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sal_pli_pl"))
    private SalPricelistEntity pricelist;

    @Column(nullable = false)
    private int sequence;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "min_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "fixed_price", precision = 19, scale = 4)
    private BigDecimal fixedPrice;

    @Column(name = "percent_discount", precision = 19, scale = 4)
    private BigDecimal percentDiscount;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SalPricelistItemEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SalPricelistEntity getPricelist() { return pricelist; }
    public void setPricelist(SalPricelistEntity pricelist) { this.pricelist = pricelist; }
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
