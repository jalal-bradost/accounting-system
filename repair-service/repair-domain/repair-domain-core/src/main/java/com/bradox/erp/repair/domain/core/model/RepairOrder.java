package com.bradox.erp.repair.domain.core.model;

import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.valueobject.RepairStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A repair order: the customer's product, the parts it needs and whether it is under warranty.
 * Confirming an order that is not under warranty creates a sale quotation for the parts.
 */
public record RepairOrder(UUID id, UUID companyId, String reference, UUID customerPartnerId, UUID productId,
                          Instant scheduledDate, boolean underWarranty, RepairStatus status,
                          UUID saleOrderId, String saleOrderName, List<Part> parts, Instant createdAt) {

    public record Part(UUID id, UUID productId, BigDecimal qty) {
    }

    public RepairOrder {
        parts = parts == null ? List.of() : List.copyOf(parts);
    }

    /** Replaces the editable fields; only a new order can be changed. */
    public RepairOrder edit(UUID customerPartnerId, UUID productId, Instant scheduledDate, boolean underWarranty, List<Part> parts) {
        requireStatus(RepairStatus.NEW, "error.repair.notEditable", "Only a new repair order can be changed");
        for (Part p : parts) {
            if (p.productId() == null || p.qty() == null || p.qty().signum() <= 0) {
                throw new RepairDomainException("error.repair.invalidPart", null, "Each part needs a product and a quantity above zero");
            }
        }
        return new RepairOrder(id, companyId, reference, customerPartnerId, productId, scheduledDate, underWarranty, status,
                saleOrderId, saleOrderName, parts, createdAt);
    }

    /** Confirms the order; the quotation is the one created for it, or null under warranty. */
    public RepairOrder confirm(UUID quotationId, String quotationName) {
        requireStatus(RepairStatus.NEW, "error.repair.notNew", "Only a new repair order can be confirmed");
        return new RepairOrder(id, companyId, reference, customerPartnerId, productId, scheduledDate, underWarranty,
                RepairStatus.CONFIRMED, quotationId, quotationName, parts, createdAt);
    }

    /** What must hold before confirming: a customer, a product, and parts to quote when not under warranty. */
    public void checkReadyToConfirm() {
        requireStatus(RepairStatus.NEW, "error.repair.notNew", "Only a new repair order can be confirmed");
        if (customerPartnerId == null || productId == null) {
            throw new RepairDomainException("error.repair.incomplete", null, "Choose the customer and the product to repair first");
        }
        if (!underWarranty && parts.isEmpty()) {
            throw new RepairDomainException("error.repair.noParts", null, "Add at least one part to create the quotation");
        }
    }

    public RepairOrder done() {
        requireStatus(RepairStatus.CONFIRMED, "error.repair.notConfirmed", "Only a confirmed repair order can be marked as done");
        return withStatus(RepairStatus.DONE);
    }

    public RepairOrder cancel() {
        if (status == RepairStatus.DONE || status == RepairStatus.CANCELLED) {
            throw new RepairDomainException("error.repair.notCancellable", null, "This repair order can no longer be cancelled");
        }
        return withStatus(RepairStatus.CANCELLED);
    }

    private RepairOrder withStatus(RepairStatus next) {
        return new RepairOrder(id, companyId, reference, customerPartnerId, productId, scheduledDate, underWarranty, next,
                saleOrderId, saleOrderName, parts, createdAt);
    }

    private void requireStatus(RepairStatus expected, String key, String message) {
        if (status != expected) {
            throw new RepairDomainException(key, null, message);
        }
    }
}
