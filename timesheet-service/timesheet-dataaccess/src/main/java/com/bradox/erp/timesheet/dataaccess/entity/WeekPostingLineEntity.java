package com.bradox.erp.timesheet.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tsh_week_posting_line")
public class WeekPostingLineEntity {

    @Id
    private UUID id;
    @Column(name = "posting_id", nullable = false)
    private UUID postingId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "debit_account_id", nullable = false)
    private UUID debitAccountId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false)
    private int minutes;

    public WeekPostingLineEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPostingId() { return postingId; }
    public void setPostingId(UUID postingId) { this.postingId = postingId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getDebitAccountId() { return debitAccountId; }
    public void setDebitAccountId(UUID debitAccountId) { this.debitAccountId = debitAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
}
