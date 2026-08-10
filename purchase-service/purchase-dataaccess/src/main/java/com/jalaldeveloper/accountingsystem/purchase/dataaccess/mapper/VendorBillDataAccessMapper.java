package com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorBillEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorBillLineEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorBillLineTaxEntity;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.VendorBillMoveType;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBill;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBillLine;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBillLineTax;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class VendorBillDataAccessMapper {

    public VendorBill entityToDomain(PurVendorBillEntity e) {
        if (e == null) return null;
        VendorBill d = new VendorBill();
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setVendorPartnerId(e.getVendorPartnerId());
        d.setPurchaseOrderId(e.getPurchaseOrderId());
        d.setBillDate(e.getBillDate());
        d.setDueDate(e.getDueDate());
        d.setReference(e.getReference());
        d.setCurrencyCode(e.getCurrencyCode());
        d.setState(e.getState());
        d.setMoveType(e.getMoveType() != null ? e.getMoveType() : VendorBillMoveType.BILL);
        d.setReversedBillId(e.getReversedBillId());
        d.setJournalEntryId(e.getJournalEntryId());
        d.setExchangeRateToCompany(e.getExchangeRateToCompany());
        d.setRowVersion(e.getRowVersion());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedBy(e.getUpdatedBy());
        if (e.getLines() != null) {
            for (PurVendorBillLineEntity lineEntity : e.getLines()) {
                d.getLines().add(lineEntityToDomain(lineEntity));
            }
        }
        return d;
    }

    private VendorBillLine lineEntityToDomain(PurVendorBillLineEntity e) {
        VendorBillLine d = new VendorBillLine();
        d.setId(e.getId());
        d.setSequence(e.getSequence());
        d.setPurchaseOrderLineId(e.getPurchaseOrderLineId());
        d.setProductId(e.getProductId());
        d.setName(e.getName());
        d.setUomId(e.getUomId());
        d.setQty(e.getQty());
        d.setUnitPrice(e.getUnitPrice());
        d.setAccountId(e.getAccountId());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        if (e.getTaxSnapshots() != null) {
            for (PurVendorBillLineTaxEntity taxEntity : e.getTaxSnapshots()) {
                VendorBillLineTax tax = new VendorBillLineTax();
                tax.setId(taxEntity.getId());
                tax.setTaxId(taxEntity.getTaxId());
                tax.setTaxName(taxEntity.getTaxName());
                tax.setTaxBase(taxEntity.getTaxBase());
                tax.setTaxAmount(taxEntity.getTaxAmount());
                tax.setAccountId(taxEntity.getAccountId());
                d.getTaxSnapshots().add(tax);
            }
        }
        return d;
    }

    public PurVendorBillEntity domainToEntity(VendorBill d, PurVendorBillEntity existingOrNull) {
        if (d == null) return null;
        PurVendorBillEntity e = existingOrNull != null ? existingOrNull : new PurVendorBillEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setVendorPartnerId(d.getVendorPartnerId());
        e.setPurchaseOrderId(d.getPurchaseOrderId());
        e.setBillDate(d.getBillDate());
        e.setDueDate(d.getDueDate());
        e.setReference(d.getReference());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setState(d.getState());
        e.setMoveType(d.getMoveType() != null ? d.getMoveType() : VendorBillMoveType.BILL);
        e.setReversedBillId(d.getReversedBillId());
        e.setJournalEntryId(d.getJournalEntryId());
        e.setExchangeRateToCompany(d.getExchangeRateToCompany());
        e.setRowVersion(d.getRowVersion());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        e.setCreatedBy(d.getCreatedBy());
        e.setUpdatedBy(d.getUpdatedBy());

        if (e.getLines() == null) e.setLines(new ArrayList<>());
        Map<UUID, PurVendorBillLineEntity> existingLines = new HashMap<>();
        for (PurVendorBillLineEntity line : e.getLines()) existingLines.put(line.getId(), line);
        e.getLines().clear();
        for (VendorBillLine lineDomain : d.getLines()) {
            UUID lineId = lineDomain.getId() != null ? lineDomain.getId() : UUID.randomUUID();
            PurVendorBillLineEntity lineEntity = existingLines.getOrDefault(lineId, new PurVendorBillLineEntity());
            lineEntity.setId(lineId);
            lineEntity.setVendorBill(e);
            lineEntity.setSequence(lineDomain.getSequence());
            lineEntity.setPurchaseOrderLineId(lineDomain.getPurchaseOrderLineId());
            lineEntity.setProductId(lineDomain.getProductId());
            lineEntity.setName(lineDomain.getName());
            lineEntity.setUomId(lineDomain.getUomId());
            lineEntity.setQty(lineDomain.getQty());
            lineEntity.setUnitPrice(lineDomain.getUnitPrice());
            lineEntity.setAccountId(lineDomain.getAccountId());
            lineEntity.setCreatedAt(lineDomain.getCreatedAt());
            lineEntity.setUpdatedAt(lineDomain.getUpdatedAt());

            if (lineEntity.getTaxSnapshots() == null) lineEntity.setTaxSnapshots(new ArrayList<>());
            Map<UUID, PurVendorBillLineTaxEntity> existingTaxes = new HashMap<>();
            for (PurVendorBillLineTaxEntity tax : lineEntity.getTaxSnapshots()) existingTaxes.put(tax.getId(), tax);
            lineEntity.getTaxSnapshots().clear();
            for (VendorBillLineTax taxDomain : lineDomain.getTaxSnapshots()) {
                UUID taxId = taxDomain.getId() != null ? taxDomain.getId() : UUID.randomUUID();
                PurVendorBillLineTaxEntity taxEntity = existingTaxes.getOrDefault(taxId, new PurVendorBillLineTaxEntity());
                taxEntity.setId(taxId);
                taxEntity.setLine(lineEntity);
                taxEntity.setTaxId(taxDomain.getTaxId());
                taxEntity.setTaxName(taxDomain.getTaxName());
                taxEntity.setTaxBase(taxDomain.getTaxBase());
                taxEntity.setTaxAmount(taxDomain.getTaxAmount());
                taxEntity.setAccountId(taxDomain.getAccountId());
                lineEntity.getTaxSnapshots().add(taxEntity);
            }
            e.getLines().add(lineEntity);
        }
        return e;
    }
}
