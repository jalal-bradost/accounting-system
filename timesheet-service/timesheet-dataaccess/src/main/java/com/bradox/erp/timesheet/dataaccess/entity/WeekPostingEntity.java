package com.bradox.erp.timesheet.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tsh_week_posting")
public class WeekPostingEntity {

    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "week_id", nullable = false)
    private UUID weekId;
    @Column(nullable = false)
    private int version;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "journal_entry_id")
    private UUID journalEntryId;
    @Column(name = "reversal_entry_id")
    private UUID reversalEntryId;
    @Column(name = "entry_date")
    private LocalDate entryDate;
    @Column(name = "late_posted", nullable = false)
    private boolean latePosted;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "posted_at")
    private Instant postedAt;

    public WeekPostingEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getWeekId() { return weekId; }
    public void setWeekId(UUID weekId) { this.weekId = weekId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public void setJournalEntryId(UUID journalEntryId) { this.journalEntryId = journalEntryId; }
    public UUID getReversalEntryId() { return reversalEntryId; }
    public void setReversalEntryId(UUID reversalEntryId) { this.reversalEntryId = reversalEntryId; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public boolean isLatePosted() { return latePosted; }
    public void setLatePosted(boolean latePosted) { this.latePosted = latePosted; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
