package com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurPurchaseOrderEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurPurchaseOrderLineEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurPurchaseOrderLineTaxEntity;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrder;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrderLine;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrderLineTax;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PurchaseOrderDataAccessMapper {

    public PurchaseOrder entityToDomain(PurPurchaseOrderEntity e) {
        if (e == null) return null;
        PurchaseOrder d = new PurchaseOrder();
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setVendorPartnerId(e.getVendorPartnerId());
        d.setName(e.getName());
        d.setState(e.getState());
        d.setCurrencyCode(e.getCurrencyCode());
        d.setWarehouseId(e.getWarehouseId());
        d.setDestLocationId(e.getDestLocationId());
        d.setPaymentTermsId(e.getPaymentTermsId());
        d.setOrderDate(e.getOrderDate());
        d.setExpectedDate(e.getExpectedDate());
        d.setIncoterm(e.getIncoterm());
        d.setNotes(e.getNotes());
        d.setVendorReference(e.getVendorReference());
        d.setSentAt(e.getSentAt());
        d.setConfirmedAt(e.getConfirmedAt());
        d.setReceivedCompletedAt(e.getReceivedCompletedAt());
        d.setBilledCompletedAt(e.getBilledCompletedAt());
        d.setCancelledAt(e.getCancelledAt());
        d.setAmountUntaxed(e.getAmountUntaxed());
        d.setAmountTax(e.getAmountTax());
        d.setAmountTotal(e.getAmountTotal());
        d.setExchangeRateToCompany(e.getExchangeRateToCompany());
        d.setRowVersion(e.getRowVersion());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedBy(e.getUpdatedBy());
        if (e.getLines() != null) {
            for (PurPurchaseOrderLineEntity lineEntity : e.getLines()) {
                d.getLines().add(lineEntityToDomain(lineEntity));
            }
        }
        return d;
    }

    private PurchaseOrderLine lineEntityToDomain(PurPurchaseOrderLineEntity e) {
        PurchaseOrderLine d = new PurchaseOrderLine();
        d.setId(e.getId());
        d.setSequence(e.getSequence());
        d.setProductId(e.getProductId());
        d.setName(e.getName());
        d.setUomId(e.getUomId());
        d.setWarehouseId(e.getWarehouseId());
        d.setQtyOrdered(e.getQtyOrdered());
        d.setQtyReceived(e.getQtyReceived());
        d.setQtyInvoiced(e.getQtyInvoiced());
        d.setUnitPrice(e.getUnitPrice());
        d.setDiscountPercent(e.getDiscountPercent());
        d.setExpectedDate(e.getExpectedDate());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        if (e.getTaxes() != null) {
            for (PurPurchaseOrderLineTaxEntity taxEntity : e.getTaxes()) {
                PurchaseOrderLineTax tax = new PurchaseOrderLineTax();
                tax.setId(taxEntity.getId());
                tax.setTaxId(taxEntity.getTaxId());
                tax.setSequence(taxEntity.getSequence());
                d.getTaxes().add(tax);
            }
        }
        return d;
    }

    public PurPurchaseOrderEntity domainToEntity(PurchaseOrder d, PurPurchaseOrderEntity existingOrNull) {
        if (d == null) return null;
        PurPurchaseOrderEntity e = existingOrNull != null ? existingOrNull : new PurPurchaseOrderEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setVendorPartnerId(d.getVendorPartnerId());
        e.setName(d.getName());
        e.setState(d.getState());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setWarehouseId(d.getWarehouseId());
        e.setDestLocationId(d.getDestLocationId());
        e.setPaymentTermsId(d.getPaymentTermsId());
        e.setOrderDate(d.getOrderDate());
        e.setExpectedDate(d.getExpectedDate());
        e.setIncoterm(d.getIncoterm());
        e.setNotes(d.getNotes());
        e.setVendorReference(d.getVendorReference());
        e.setSentAt(d.getSentAt());
        e.setConfirmedAt(d.getConfirmedAt());
        e.setReceivedCompletedAt(d.getReceivedCompletedAt());
        e.setBilledCompletedAt(d.getBilledCompletedAt());
        e.setCancelledAt(d.getCancelledAt());
        e.setAmountUntaxed(d.getAmountUntaxed());
        e.setAmountTax(d.getAmountTax());
        e.setAmountTotal(d.getAmountTotal());
        e.setExchangeRateToCompany(d.getExchangeRateToCompany());
        e.setRowVersion(d.getRowVersion());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        e.setCreatedBy(d.getCreatedBy());
        e.setUpdatedBy(d.getUpdatedBy());

        if (e.getLines() == null) e.setLines(new ArrayList<>());
        Map<UUID, PurPurchaseOrderLineEntity> existingLines = new HashMap<>();
        for (PurPurchaseOrderLineEntity line : e.getLines()) existingLines.put(line.getId(), line);
        e.getLines().clear();
        for (PurchaseOrderLine lineDomain : d.getLines()) {
            UUID lineId = lineDomain.getId() != null ? lineDomain.getId() : UUID.randomUUID();
            PurPurchaseOrderLineEntity lineEntity = existingLines.getOrDefault(lineId, new PurPurchaseOrderLineEntity());
            lineEntity.setId(lineId);
            lineEntity.setPurchaseOrder(e);
            lineEntity.setSequence(lineDomain.getSequence());
            lineEntity.setProductId(lineDomain.getProductId());
            lineEntity.setName(lineDomain.getName());
            lineEntity.setUomId(lineDomain.getUomId());
            lineEntity.setWarehouseId(lineDomain.getWarehouseId());
            lineEntity.setQtyOrdered(lineDomain.getQtyOrdered());
            lineEntity.setQtyReceived(lineDomain.getQtyReceived());
            lineEntity.setQtyInvoiced(lineDomain.getQtyInvoiced());
            lineEntity.setUnitPrice(lineDomain.getUnitPrice());
            lineEntity.setDiscountPercent(lineDomain.getDiscountPercent());
            lineEntity.setExpectedDate(lineDomain.getExpectedDate());
            lineEntity.setCreatedAt(lineDomain.getCreatedAt());
            lineEntity.setUpdatedAt(lineDomain.getUpdatedAt());

            if (lineEntity.getTaxes() == null) lineEntity.setTaxes(new ArrayList<>());
            Map<UUID, PurPurchaseOrderLineTaxEntity> existingTaxes = new HashMap<>();
            for (PurPurchaseOrderLineTaxEntity tax : lineEntity.getTaxes()) existingTaxes.put(tax.getId(), tax);
            lineEntity.getTaxes().clear();
            for (PurchaseOrderLineTax taxDomain : lineDomain.getTaxes()) {
                UUID taxId = taxDomain.getId() != null ? taxDomain.getId() : UUID.randomUUID();
                PurPurchaseOrderLineTaxEntity taxEntity = existingTaxes.getOrDefault(taxId, new PurPurchaseOrderLineTaxEntity());
                taxEntity.setId(taxId);
                taxEntity.setLine(lineEntity);
                taxEntity.setTaxId(taxDomain.getTaxId());
                taxEntity.setSequence(taxDomain.getSequence());
                lineEntity.getTaxes().add(taxEntity);
            }
            e.getLines().add(lineEntity);
        }
        return e;
    }
}
