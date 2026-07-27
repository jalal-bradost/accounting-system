package com.jalaldeveloper.accountingsystem.hr.dataaccess.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pay_salary_rule")
public class PaySalaryRuleEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id", nullable = false)
    private PayStructureEntity structure;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(name = "amount_type", nullable = false, length = 32)
    private String amountType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "account_id")
    private UUID accountId;

    public PaySalaryRuleEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PayStructureEntity getStructure() { return structure; }
    public void setStructure(PayStructureEntity structure) { this.structure = structure; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAmountType() { return amountType; }
    public void setAmountType(String amountType) { this.amountType = amountType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
}
