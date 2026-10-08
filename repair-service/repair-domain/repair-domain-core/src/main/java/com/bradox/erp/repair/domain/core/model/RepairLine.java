package com.bradox.erp.repair.domain.core.model;

import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.valueobject.Disposition;
import com.bradox.erp.repair.domain.core.valueobject.LineStatus;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.domain.core.valueobject.PricingMode;
import com.bradox.erp.repair.domain.core.valueobject.SubletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One thing to do or supply on a repair order. Prices are kept here until the order is backed by a sales order (D3);
 * the pure maths lives in {@link com.bradox.erp.repair.domain.core.rule.LaborPricing}.
 */
public record RepairLine(UUID id, UUID companyId, UUID orderId, LineType type, String sectionLabel, int sequence, UUID productId,
                         String description, BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPercent, LineStatus status,
                         boolean needsDiscountApproval, UUID fromFindingId, UUID fromPackageId, UUID laborGuideId,
                         PricingMode pricingMode, Integer standardMinutes, BigDecimal laborRate, UUID assigneeEmployeeId,
                         UUID vendorPartnerId, BigDecimal vendorCost, SubletStatus subletStatus, Disposition disposition,
                         Instant createdAt, String createdBy) {

    public RepairLine {
        if (description == null || description.isBlank()) {
            throw new RepairDomainException("error.repair.line.description", null, "A line needs a description");
        }
        if (qty == null || qty.signum() <= 0) {
            throw new RepairDomainException("error.repair.line.qty", null, "Quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new RepairDomainException("error.repair.line.price", null, "Price cannot be negative");
        }
        if (discountPercent == null || discountPercent.signum() < 0 || discountPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RepairDomainException("error.repair.line.discount", null, "Discount must be between 0 and 100 percent");
        }
        if ((type == LineType.CUSTOMER_PART || type == LineType.REMOVED_PART)
                && (unitPrice.signum() != 0 || discountPercent.signum() != 0)) {
            throw new RepairDomainException("error.repair.line.noPrice", null, "Customer parts and removed parts have no price");
        }
    }

    /** Only DRAFT lines may be edited or removed; later changes follow the quotation rules (REP-04). */
    public boolean isEditable() {
        return status == LineStatus.DRAFT;
    }

    public RepairLine with(String sectionLabel, int sequence, String description, BigDecimal qty, BigDecimal unitPrice,
                           BigDecimal discountPercent, boolean needsDiscountApproval) {
        return new RepairLine(id, companyId, orderId, type, sectionLabel, sequence, productId, description, qty, unitPrice,
                discountPercent, status, needsDiscountApproval, fromFindingId, fromPackageId, laborGuideId, pricingMode,
                standardMinutes, laborRate, assigneeEmployeeId, vendorPartnerId, vendorCost, subletStatus, disposition, createdAt,
                createdBy);
    }

    public RepairLine withSublet(UUID vendorPartnerId, BigDecimal vendorCost, SubletStatus subletStatus) {
        return new RepairLine(id, companyId, orderId, type, sectionLabel, sequence, productId, description, qty, unitPrice,
                discountPercent, status, needsDiscountApproval, fromFindingId, fromPackageId, laborGuideId, pricingMode,
                standardMinutes, laborRate, assigneeEmployeeId, vendorPartnerId, vendorCost, subletStatus, disposition, createdAt,
                createdBy);
    }

    public RepairLine withDisposition(Disposition disposition) {
        return new RepairLine(id, companyId, orderId, type, sectionLabel, sequence, productId, description, qty, unitPrice,
                discountPercent, status, needsDiscountApproval, fromFindingId, fromPackageId, laborGuideId, pricingMode,
                standardMinutes, laborRate, assigneeEmployeeId, vendorPartnerId, vendorCost, subletStatus, disposition, createdAt,
                createdBy);
    }

    public RepairLine withApproved() {
        return new RepairLine(id, companyId, orderId, type, sectionLabel, sequence, productId, description, qty, unitPrice,
                discountPercent, status, false, fromFindingId, fromPackageId, laborGuideId, pricingMode, standardMinutes,
                laborRate, assigneeEmployeeId, vendorPartnerId, vendorCost, subletStatus, disposition, createdAt, createdBy);
    }
}
