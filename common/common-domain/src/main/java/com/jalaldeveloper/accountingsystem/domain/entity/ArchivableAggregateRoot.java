package com.jalaldeveloper.accountingsystem.domain.entity;

import java.time.Instant;

/**
 * Aggregate root that supports Odoo-style soft delete via an {@code active} flag.
 * Concrete entities use {@link #archive(String)} / {@link #unarchive()} rather than
 * deleting rows so historical references (FKs, audit logs, journal entries) stay intact.
 */
public abstract class ArchivableAggregateRoot<ID> extends AggregateRoot<ID> {

    private boolean active = true;
    private Instant archivedAt;
    private String archivedBy;

    public boolean isActive() {
        return active;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public String getArchivedBy() {
        return archivedBy;
    }

    /** Mark this aggregate as archived. Idempotent. */
    public void archive(String userId) {
        if (!this.active) return;
        this.active = false;
        this.archivedAt = Instant.now();
        this.archivedBy = userId;
    }

    /** Restore an archived aggregate. Idempotent. */
    public void unarchive() {
        if (this.active) return;
        this.active = true;
        this.archivedAt = null;
        this.archivedBy = null;
    }

    /** Used by data-access mappers to rehydrate state from persistence. */
    protected void restoreArchiveState(boolean active, Instant archivedAt, String archivedBy) {
        this.active = active;
        this.archivedAt = archivedAt;
        this.archivedBy = archivedBy;
    }
}
