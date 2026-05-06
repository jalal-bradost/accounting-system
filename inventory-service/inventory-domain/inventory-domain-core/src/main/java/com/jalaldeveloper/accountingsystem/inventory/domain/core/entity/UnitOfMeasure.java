package com.jalaldeveloper.accountingsystem.inventory.domain.core.entity;

import com.jalaldeveloper.accountingsystem.domain.entity.ArchivableAggregateRoot;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomCategoryId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomId;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject.UomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * A unit within a {@link UomCategory}. The reference unit has factor 1; bigger units have a
 * factor &gt; 1 (e.g. dozen = 12), smaller units a factor &lt; 1 (e.g. gram = 0.001 vs kg).
 *
 * <p>Conversions multiply by the source factor and divide by the target factor:
 * {@code qtyTarget = qtySource * sourceFactor / targetFactor}.
 */
public class UnitOfMeasure extends ArchivableAggregateRoot<UomId> {

    private static final int CONVERSION_SCALE = 6;

    private final CompanyId companyId;
    private final UomCategoryId categoryId;
    private String name;
    private UomType uomType;
    private BigDecimal factor;
    private int rounding;

    private UnitOfMeasure(Builder b) {
        super.setId(b.id);
        this.companyId = b.companyId;
        this.categoryId = b.categoryId;
        this.name = b.name;
        this.uomType = b.uomType;
        this.factor = b.factor;
        this.rounding = b.rounding > 0 ? b.rounding : 4;
        if (b.archived) {
            super.restoreArchiveState(false, b.archivedAt, b.archivedBy);
        }
    }

    public void validate() {
        if (companyId == null) throw new InventoryDomainException("companyId required");
        if (categoryId == null) throw new InventoryDomainException("uom categoryId required");
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
        if (uomType == null) throw new InventoryDomainException("uomType required");
        if (factor == null || factor.signum() <= 0) {
            throw new InventoryDomainException("factor must be > 0");
        }
        if (uomType == UomType.REFERENCE && factor.compareTo(BigDecimal.ONE) != 0) {
            throw new InventoryDomainException("REFERENCE uom factor must be exactly 1");
        }
        if (uomType == UomType.BIGGER && factor.compareTo(BigDecimal.ONE) <= 0) {
            throw new InventoryDomainException("BIGGER uom factor must be > 1");
        }
        if (uomType == UomType.SMALLER && factor.compareTo(BigDecimal.ONE) >= 0) {
            throw new InventoryDomainException("SMALLER uom factor must be < 1");
        }
    }

    /** Convert {@code qty} expressed in {@code this} unit to {@code target} unit. */
    public BigDecimal convertTo(BigDecimal qty, UnitOfMeasure target) {
        if (qty == null) return null;
        if (target == null) throw new InventoryDomainException("target uom required");
        if (!Objects.equals(this.categoryId, target.categoryId)) {
            throw new InventoryDomainException(
                    "Cannot convert between UoMs in different categories: " + this.name + " vs " + target.name);
        }
        if (this.equals(target)) return qty.setScale(target.rounding, RoundingMode.HALF_UP);
        BigDecimal asReference = qty.multiply(this.factor);
        return asReference.divide(target.factor, target.rounding, RoundingMode.HALF_UP);
    }

    /** Round {@code qty} to this UoM's rounding precision (HALF_UP). */
    public BigDecimal round(BigDecimal qty) {
        if (qty == null) return null;
        return qty.setScale(rounding, RoundingMode.HALF_UP);
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new InventoryDomainException("name required");
        this.name = name;
    }

    public void changeFactor(BigDecimal factor) {
        if (factor == null || factor.signum() <= 0) {
            throw new InventoryDomainException("factor must be > 0");
        }
        this.factor = factor;
        validate();
    }

    public void changeRounding(int rounding) {
        if (rounding < 0 || rounding > CONVERSION_SCALE) {
            throw new InventoryDomainException("rounding must be in [0," + CONVERSION_SCALE + "]");
        }
        this.rounding = rounding;
    }

    public CompanyId getCompanyId() { return companyId; }
    public UomCategoryId getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public UomType getUomType() { return uomType; }
    public BigDecimal getFactor() { return factor; }
    public int getRounding() { return rounding; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UomId id;
        private CompanyId companyId;
        private UomCategoryId categoryId;
        private String name;
        private UomType uomType;
        private BigDecimal factor;
        private int rounding;
        private boolean archived;
        private Instant archivedAt;
        private String archivedBy;

        public Builder id(UomId v) { this.id = v; return this; }
        public Builder companyId(CompanyId v) { this.companyId = v; return this; }
        public Builder categoryId(UomCategoryId v) { this.categoryId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder uomType(UomType v) { this.uomType = v; return this; }
        public Builder factor(BigDecimal v) { this.factor = v; return this; }
        public Builder rounding(int v) { this.rounding = v; return this; }
        public Builder archived(boolean v) { this.archived = v; return this; }
        public Builder archivedAt(Instant v) { this.archivedAt = v; return this; }
        public Builder archivedBy(String v) { this.archivedBy = v; return this; }
        public UnitOfMeasure build() { return new UnitOfMeasure(this); }
    }
}
