package com.jalaldeveloper.accountingsystem.pos.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosConfigEntity;
import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosOrderEntity;
import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosOrderLineEntity;
import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosPaymentEntity;
import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosReceiptEntity;
import com.jalaldeveloper.accountingsystem.pos.dataaccess.entity.PosSessionEntity;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosConfig;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosOrder;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosOrderLine;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosPayment;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosReceipt;
import com.jalaldeveloper.accountingsystem.pos.domain.core.entity.PosSession;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PosDataAccessMapper {

    public PosConfig entityToDomain(PosConfigEntity e) {
        if (e == null) return null;
        PosConfig d = new PosConfig();
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setName(e.getName());
        d.setWarehouseId(e.getWarehouseId());
        d.setDefaultCustomerPartnerId(e.getDefaultCustomerPartnerId());
        d.setCashJournalId(e.getCashJournalId());
        d.setBankJournalId(e.getBankJournalId());
        d.setPricelistId(e.getPricelistId());
        d.setCurrencyCode(e.getCurrencyCode());
        d.setActive(e.isActive());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public PosConfigEntity domainToEntity(PosConfig d, PosConfigEntity existingOrNull) {
        if (d == null) return null;
        PosConfigEntity e = existingOrNull != null ? existingOrNull : new PosConfigEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setName(d.getName());
        e.setWarehouseId(d.getWarehouseId());
        e.setDefaultCustomerPartnerId(d.getDefaultCustomerPartnerId());
        e.setCashJournalId(d.getCashJournalId());
        e.setBankJournalId(d.getBankJournalId());
        e.setPricelistId(d.getPricelistId());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setActive(d.isActive());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    public PosSession entityToDomain(PosSessionEntity e) {
        if (e == null) return null;
        PosSession d = new PosSession();
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setConfigId(e.getConfigId());
        d.setState(e.getState());
        d.setWarehouseId(e.getWarehouseId());
        d.setDefaultCustomerPartnerId(e.getDefaultCustomerPartnerId());
        d.setCashJournalId(e.getCashJournalId());
        d.setBankJournalId(e.getBankJournalId());
        d.setPricelistId(e.getPricelistId());
        d.setCurrencyCode(e.getCurrencyCode());
        d.setOpeningCash(e.getOpeningCash());
        d.setClosingCash(e.getClosingCash());
        d.setOpenedAt(e.getOpenedAt());
        d.setClosedAt(e.getClosedAt());
        d.setRowVersion(e.getRowVersion());
        return d;
    }

    public PosSessionEntity domainToEntity(PosSession d, PosSessionEntity existingOrNull) {
        if (d == null) return null;
        PosSessionEntity e = existingOrNull != null ? existingOrNull : new PosSessionEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setConfigId(d.getConfigId());
        e.setState(d.getState());
        e.setWarehouseId(d.getWarehouseId());
        e.setDefaultCustomerPartnerId(d.getDefaultCustomerPartnerId());
        e.setCashJournalId(d.getCashJournalId());
        e.setBankJournalId(d.getBankJournalId());
        e.setPricelistId(d.getPricelistId());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setOpeningCash(d.getOpeningCash());
        e.setClosingCash(d.getClosingCash());
        e.setOpenedAt(d.getOpenedAt());
        e.setClosedAt(d.getClosedAt());
        if (existingOrNull != null) {
            e.setRowVersion(d.getRowVersion());
        }
        return e;
    }

    public PosOrder entityToDomain(PosOrderEntity e) {
        if (e == null) return null;
        PosOrder d = new PosOrder();
        mapOrderScalars(e, d);
        for (PosOrderLineEntity lineEntity : e.getLines()) {
            PosOrderLine line = entityToDomain(lineEntity);
            line.setOrder(d);
            d.getLines().add(line);
        }
        for (PosPaymentEntity paymentEntity : e.getPayments()) {
            PosPayment payment = entityToDomain(paymentEntity);
            payment.setOrder(d);
            d.getPayments().add(payment);
        }
        return d;
    }

    public PosOrderEntity domainToEntity(PosOrder d, PosOrderEntity existingOrNull) {
        if (d == null) return null;
        PosOrderEntity e = existingOrNull != null ? existingOrNull : new PosOrderEntity();
        mapOrderScalars(d, e);
        if (existingOrNull != null) {
            e.setRowVersion(d.getRowVersion());
        }
        e.getLines().clear();
        for (PosOrderLine line : d.getLines()) {
            PosOrderLineEntity lineEntity = domainToEntity(line, null);
            lineEntity.setOrder(e);
            e.getLines().add(lineEntity);
        }
        e.getPayments().clear();
        for (PosPayment payment : d.getPayments()) {
            PosPaymentEntity paymentEntity = domainToEntity(payment, null);
            paymentEntity.setOrder(e);
            e.getPayments().add(paymentEntity);
        }
        return e;
    }

    public PosOrderLine entityToDomain(PosOrderLineEntity e) {
        if (e == null) return null;
        PosOrderLine d = new PosOrderLine();
        d.setId(e.getId());
        d.setSequence(e.getSequence());
        d.setProductId(e.getProductId());
        d.setName(e.getName());
        d.setUomId(e.getUomId());
        d.setQuantity(e.getQuantity());
        d.setUnitPrice(e.getUnitPrice());
        d.setDiscountPercent(e.getDiscountPercent());
        d.setSubtotal(e.getSubtotal());
        d.setTaxAmount(e.getTaxAmount());
        d.setTotal(e.getTotal());
        d.setRevenueAccountId(e.getRevenueAccountId());
        d.setTaxIds(new ArrayList<>(e.getTaxIds()));
        return d;
    }

    public PosOrderLineEntity domainToEntity(PosOrderLine d, PosOrderLineEntity existingOrNull) {
        if (d == null) return null;
        PosOrderLineEntity e = existingOrNull != null ? existingOrNull : new PosOrderLineEntity();
        e.setId(d.getId());
        e.setSequence(d.getSequence());
        e.setProductId(d.getProductId());
        e.setName(d.getName());
        e.setUomId(d.getUomId());
        e.setQuantity(d.getQuantity());
        e.setUnitPrice(d.getUnitPrice());
        e.setDiscountPercent(d.getDiscountPercent());
        e.setSubtotal(d.getSubtotal());
        e.setTaxAmount(d.getTaxAmount());
        e.setTotal(d.getTotal());
        e.setRevenueAccountId(d.getRevenueAccountId());
        e.setTaxIds(new ArrayList<>(d.getTaxIds()));
        return e;
    }

    public PosPayment entityToDomain(PosPaymentEntity e) {
        if (e == null) return null;
        PosPayment d = new PosPayment();
        d.setId(e.getId());
        d.setMethod(e.getMethod());
        d.setJournalId(e.getJournalId());
        d.setAmount(e.getAmount());
        d.setReference(e.getReference());
        d.setPaidAt(e.getPaidAt());
        return d;
    }

    public PosPaymentEntity domainToEntity(PosPayment d, PosPaymentEntity existingOrNull) {
        if (d == null) return null;
        PosPaymentEntity e = existingOrNull != null ? existingOrNull : new PosPaymentEntity();
        e.setId(d.getId());
        e.setMethod(d.getMethod());
        e.setJournalId(d.getJournalId());
        e.setAmount(d.getAmount());
        e.setReference(d.getReference());
        e.setPaidAt(d.getPaidAt());
        return e;
    }

    public PosReceipt entityToDomain(PosReceiptEntity e) {
        if (e == null) return null;
        PosReceipt d = new PosReceipt();
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setOrderId(e.getOrderId());
        d.setReceiptNumber(e.getReceiptNumber());
        d.setPayloadJson(e.getPayloadJson());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }

    public PosReceiptEntity domainToEntity(PosReceipt d, PosReceiptEntity existingOrNull) {
        if (d == null) return null;
        PosReceiptEntity e = existingOrNull != null ? existingOrNull : new PosReceiptEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setOrderId(d.getOrderId());
        e.setReceiptNumber(d.getReceiptNumber());
        e.setPayloadJson(d.getPayloadJson());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    private static void mapOrderScalars(PosOrderEntity e, PosOrder d) {
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setSessionId(e.getSessionId());
        d.setCustomerPartnerId(e.getCustomerPartnerId());
        d.setName(e.getName());
        d.setState(e.getState());
        d.setCurrencyCode(e.getCurrencyCode());
        d.setAmountUntaxed(e.getAmountUntaxed());
        d.setAmountTax(e.getAmountTax());
        d.setAmountTotal(e.getAmountTotal());
        d.setAmountPaid(e.getAmountPaid());
        d.setNote(e.getNote());
        d.setSalesOrderId(e.getSalesOrderId());
        d.setCustomerInvoiceId(e.getCustomerInvoiceId());
        d.setReceiptId(e.getReceiptId());
        d.setFinalizedAt(e.getFinalizedAt());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        d.setRowVersion(e.getRowVersion());
    }

    private static void mapOrderScalars(PosOrder d, PosOrderEntity e) {
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setSessionId(d.getSessionId());
        e.setCustomerPartnerId(d.getCustomerPartnerId());
        e.setName(d.getName());
        e.setState(d.getState());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setAmountUntaxed(d.getAmountUntaxed());
        e.setAmountTax(d.getAmountTax());
        e.setAmountTotal(d.getAmountTotal());
        e.setAmountPaid(d.getAmountPaid());
        e.setNote(d.getNote());
        e.setSalesOrderId(d.getSalesOrderId());
        e.setCustomerInvoiceId(d.getCustomerInvoiceId());
        e.setReceiptId(d.getReceiptId());
        e.setFinalizedAt(d.getFinalizedAt());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
    }
}
