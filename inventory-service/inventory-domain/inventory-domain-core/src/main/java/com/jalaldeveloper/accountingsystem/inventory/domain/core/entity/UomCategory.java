package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;

import java.time.Instant;

/**
 * Groups commensurable units (e.g. Mass: g, kg, t). Conversions are only valid between units
 * of the same category.
 */
public class UomCategory extends ArchivableAggregateRoot<UomCategoryId> {

    private final CompanyId companyId;
    private String name;

    private UomCategory(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.name = b.name;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
        this.name = name;
    }

    public CompanyId getCompanyId() { return companyId; }
    public String getName() { return name; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UomCategoryId id;
        private CompanyId companyId;
        private String name;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(UomCategoryId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public UomCategory build() { return new UomCategory(this); }
    }
}
