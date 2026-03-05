package com.jalaldeveloper.accountingsystem.domain.core.ValueObject;

public enum JournalEntryStatus {
    DRAFT,
    POSTED,
    CANCELLED;

    public boolean isPosted() {
        return this == POSTED;
    }

    public boolean isModifiable() {
        return this == DRAFT;
    }
}