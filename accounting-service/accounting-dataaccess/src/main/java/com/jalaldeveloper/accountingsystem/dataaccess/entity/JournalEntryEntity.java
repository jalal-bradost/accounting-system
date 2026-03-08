package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntryEntity {
    @Id
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalEntity journal;
    @Column(name = "sequence_number", nullable = false)
    private String sequenceNumber;
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalEntryStatus status;
    @Column(name = "reversal_of_entry_id")
    private UUID reversalOfEntryId;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "posted_at")
    private Instant postedAt;
    @Column(name = "posted_by", length = 255)
    private String postedBy;
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalItemEntity> items = new ArrayList<>();

    public JournalEntryEntity() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public JournalEntity getJournal() { return journal; }
    public void setJournal(JournalEntity journal) { this.journal = journal; }
    public String getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(String sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public JournalEntryStatus getStatus() { return status; }
    public void setStatus(JournalEntryStatus status) { this.status = status; }
    public List<JournalItemEntity> getItems() { return items; }
    public void setItems(List<JournalItemEntity> items) { this.items = items != null ? items : new ArrayList<>(); }
    public UUID getReversalOfEntryId() { return reversalOfEntryId; }
    public void setReversalOfEntryId(UUID reversalOfEntryId) { this.reversalOfEntryId = reversalOfEntryId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }
}