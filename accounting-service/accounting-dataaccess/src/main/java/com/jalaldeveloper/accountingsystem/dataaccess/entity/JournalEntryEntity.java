package com.jalaldeveloper.accountingsystem.dataaccess.entity;

import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalEntryStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntryEntity {
    @Id
    private UUID id;
    private UUID companyId;
    
    @ManyToOne
    @JoinColumn(name = "journal_id")
    private JournalEntity journal;

    private String sequenceNumber;
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    private JournalEntryStatus status;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL)
    private List<JournalItemEntity> items;
}