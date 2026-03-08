package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "journal_entry_sequences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "journal_id", "period_key"}))
public class JournalEntrySequenceEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "journal_id", nullable = false)
    private UUID journalId;
    @Column(name = "period_key", nullable = false, length = 20)
    private String periodKey;
    @Column(name = "last_number", nullable = false)
    private long lastNumber;

    public JournalEntrySequenceEntity() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }
    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }
    public long getLastNumber() { return lastNumber; }
    public void setLastNumber(long lastNumber) { this.lastNumber = lastNumber; }
}
