package com.jalaldeveloper.accountingsystem.platform.dataaccess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;

/**
 * Mapped-superclass providing the Odoo-style {@code active} soft-delete columns.
 * JPA entities representing archivable aggregates extend this class.
 */
@MappedSuperclass
public abstract class ArchivableEntity {

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by", length = 255)
    private String archivedBy;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }

    public String getArchivedBy() { return archivedBy; }
    public void setArchivedBy(String archivedBy) { this.archivedBy = archivedBy; }
}
