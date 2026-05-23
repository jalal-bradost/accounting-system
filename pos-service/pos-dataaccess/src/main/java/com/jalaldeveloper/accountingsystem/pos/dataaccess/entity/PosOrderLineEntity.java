package com.jalaldeveloper.accountingsystem.pos.dataaccess.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pos_order_line")
public class PosOrderLineEntity {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PosOrderEntity order;
    @Column(nullable = false)
    private int sequence;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(name = "uom_id", nullable = false)
    private UUID uomId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;
    @Column(name = "discount_percent", nullable = false, precision = 9, scale = 4)
    private BigDecimal discountPercent;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;
    @Column(name = "revenue_account_id")
    private UUID revenueAccountId;
    @ElementCollection
    @CollectionTable(name = "pos_order_line_tax", joinColumns = @JoinColumn(name = "line_id"))
    @Column(name = "tax_id", nullable = false)
    @OrderColumn(name = "sequence")
    private List<UUID> taxIds = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PosOrderEntity getOrder() { return order; }
    public void setOrder(PosOrderEntity order) { this.order = order; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getUomId() { return uomId; }
    public void setUomId(UUID uomId) { this.uomId = uomId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public UUID getRevenueAccountId() { return revenueAccountId; }
    public void setRevenueAccountId(UUID revenueAccountId) { this.revenueAccountId = revenueAccountId; }
    public List<UUID> getTaxIds() { return taxIds; }
    public void setTaxIds(List<UUID> taxIds) { this.taxIds = taxIds != null ? taxIds : new ArrayList<>(); }
}
