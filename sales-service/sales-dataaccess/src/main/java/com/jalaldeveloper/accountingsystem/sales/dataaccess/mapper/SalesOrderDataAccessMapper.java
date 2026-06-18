package com.jalaldeveloper.accountingsystem.sales.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalPricelistEntity;
import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalPricelistItemEntity;
import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalSalesOrderEntity;
import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalSalesOrderLineEntity;
import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalSalesOrderLineTaxEntity;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.Pricelist;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.PricelistItem;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrder;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrderLine;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrderLineTax;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SalesOrderDataAccessMapper {

    public SalesOrder entityToDomain(SalSalesOrderEntity e) {
        if (e == null) {
            return null;
        }
        SalesOrder o = new SalesOrder();
        o.setId(e.getId());
        o.setCompanyId(e.getCompanyId());
        o.setCustomerPartnerId(e.getCustomerPartnerId());
        o.setName(e.getName());
        o.setState(e.getState());
        o.setDeliveryStatus(e.getDeliveryStatus());
        o.setInvoiceStatus(e.getInvoiceStatus());
        o.setOrderDate(e.getOrderDate());
        o.setValidityDate(e.getValidityDate());
        o.setWarehouseId(e.getWarehouseId());
        o.setPricelistId(e.getPricelistId());
        o.setPaymentTermsId(e.getPaymentTermsId());
        o.setCurrencyCode(e.getCurrencyCode());
        o.setExchangeRateToCompany(e.getExchangeRateToCompany());
        o.setIncoterm(e.getIncoterm());
        o.setNotes(e.getNotes());
        o.setAmountUntaxed(e.getAmountUntaxed());
        o.setAmountTax(e.getAmountTax());
        o.setAmountTotal(e.getAmountTotal());
        o.setQuotationSentAt(e.getQuotationSentAt());
        o.setConfirmedAt(e.getConfirmedAt());
        o.setCancelledAt(e.getCancelledAt());
        o.setDeliveryCompletedAt(e.getDeliveryCompletedAt());
        o.setInvoicingCompletedAt(e.getInvoicingCompletedAt());
        o.setRowVersion(e.getRowVersion());
        o.setCreatedAt(e.getCreatedAt());
        o.setUpdatedAt(e.getUpdatedAt());
        o.setCreatedBy(e.getCreatedBy());
        o.setUpdatedBy(e.getUpdatedBy());
        List<SalesOrderLine> lines = new ArrayList<>();
        if (e.getLines() != null) {
            for (SalSalesOrderLineEntity lineEntity : e.getLines()) {
                lines.add(lineEntityToDomain(lineEntity));
            }
        }
        o.setLines(lines);
        return o;
    }

    private SalesOrderLine lineEntityToDomain(SalSalesOrderLineEntity e) {
        SalesOrderLine line = new SalesOrderLine();
        line.setId(e.getId());
        line.setSequence(e.getSequence());
        line.setProductId(e.getProductId());
        line.setName(e.getName());
        line.setUomId(e.getUomId());
        line.setQtyOrdered(e.getQtyOrdered());
        line.setQtyDelivered(e.getQtyDelivered());
        line.setQtyInvoiced(e.getQtyInvoiced());
        line.setUnitPrice(e.getUnitPrice());
        line.setDiscountPercent(e.getDiscountPercent());
        line.setInvoicePolicy(e.getInvoicePolicy());
        line.setRevenueAccountId(e.getRevenueAccountId());
        line.setCreatedAt(e.getCreatedAt());
        line.setUpdatedAt(e.getUpdatedAt());
        List<SalesOrderLineTax> taxes = new ArrayList<>();
        if (e.getTaxes() != null) {
            for (SalSalesOrderLineTaxEntity taxEntity : e.getTaxes()) {
                taxes.add(taxEntityToDomain(taxEntity));
            }
        }
        line.setTaxes(taxes);
        return line;
    }

    private SalesOrderLineTax taxEntityToDomain(SalSalesOrderLineTaxEntity e) {
        SalesOrderLineTax tax = new SalesOrderLineTax();
        tax.setId(e.getId());
        tax.setTaxId(e.getTaxId());
        tax.setSequence(e.getSequence());
        return tax;
    }

    public SalSalesOrderEntity domainToEntity(SalesOrder o, SalSalesOrderEntity existingOrNull) {
        if (o == null) {
            return null;
        }
        SalSalesOrderEntity e = existingOrNull != null ? existingOrNull : new SalSalesOrderEntity();
        e.setId(o.getId());
        e.setCompanyId(o.getCompanyId());
        e.setCustomerPartnerId(o.getCustomerPartnerId());
        e.setName(o.getName());
        e.setState(o.getState());
        e.setDeliveryStatus(o.getDeliveryStatus());
        e.setInvoiceStatus(o.getInvoiceStatus());
        e.setOrderDate(o.getOrderDate());
        e.setValidityDate(o.getValidityDate());
        e.setWarehouseId(o.getWarehouseId());
        e.setPricelistId(o.getPricelistId());
        e.setPaymentTermsId(o.getPaymentTermsId());
        e.setCurrencyCode(o.getCurrencyCode());
        e.setExchangeRateToCompany(o.getExchangeRateToCompany());
        e.setIncoterm(o.getIncoterm());
        e.setNotes(o.getNotes());
        e.setAmountUntaxed(o.getAmountUntaxed());
        e.setAmountTax(o.getAmountTax());
        e.setAmountTotal(o.getAmountTotal());
        e.setQuotationSentAt(o.getQuotationSentAt());
        e.setConfirmedAt(o.getConfirmedAt());
        e.setCancelledAt(o.getCancelledAt());
        e.setDeliveryCompletedAt(o.getDeliveryCompletedAt());
        e.setInvoicingCompletedAt(o.getInvoicingCompletedAt());
        e.setCreatedAt(o.getCreatedAt());
        e.setUpdatedAt(o.getUpdatedAt());
        e.setCreatedBy(o.getCreatedBy());
        e.setUpdatedBy(o.getUpdatedBy());

        if (e.getLines() == null) {
            e.setLines(new ArrayList<>());
        }
        Map<UUID, SalSalesOrderLineEntity> existingLines = new HashMap<>();
        for (SalSalesOrderLineEntity lineEntity : e.getLines()) {
            existingLines.put(lineEntity.getId(), lineEntity);
        }
        e.getLines().clear();
        for (SalesOrderLine line : o.getLines()) {
            UUID lineId = line.getId() != null ? line.getId() : UUID.randomUUID();
            SalSalesOrderLineEntity lineEntity = existingLines.getOrDefault(lineId, new SalSalesOrderLineEntity());
            lineEntity.setId(lineId);
            lineEntity.setSalesOrder(e);
            lineEntity.setSequence(line.getSequence());
            lineEntity.setProductId(line.getProductId());
            lineEntity.setName(line.getName());
            lineEntity.setUomId(line.getUomId());
            lineEntity.setQtyOrdered(line.getQtyOrdered());
            lineEntity.setQtyDelivered(line.getQtyDelivered());
            lineEntity.setQtyInvoiced(line.getQtyInvoiced());
            lineEntity.setUnitPrice(line.getUnitPrice());
            lineEntity.setDiscountPercent(line.getDiscountPercent());
            lineEntity.setInvoicePolicy(line.getInvoicePolicy());
            lineEntity.setRevenueAccountId(line.getRevenueAccountId());
            lineEntity.setCreatedAt(line.getCreatedAt());
            lineEntity.setUpdatedAt(line.getUpdatedAt());

            if (lineEntity.getTaxes() == null) {
                lineEntity.setTaxes(new ArrayList<>());
            }
            Map<UUID, SalSalesOrderLineTaxEntity> existingTaxes = new HashMap<>();
            for (SalSalesOrderLineTaxEntity taxEntity : lineEntity.getTaxes()) {
                existingTaxes.put(taxEntity.getId(), taxEntity);
            }
            lineEntity.getTaxes().clear();
            for (SalesOrderLineTax tax : line.getTaxes()) {
                UUID taxId = tax.getId() != null ? tax.getId() : UUID.randomUUID();
                SalSalesOrderLineTaxEntity taxEntity = existingTaxes.getOrDefault(taxId, new SalSalesOrderLineTaxEntity());
                taxEntity.setId(taxId);
                taxEntity.setLine(lineEntity);
                taxEntity.setTaxId(tax.getTaxId());
                taxEntity.setSequence(tax.getSequence());
                lineEntity.getTaxes().add(taxEntity);
            }
            e.getLines().add(lineEntity);
        }
        return e;
    }

    public Pricelist pricelistEntityToDomain(SalPricelistEntity e) {
        if (e == null) {
            return null;
        }
        Pricelist pl = new Pricelist();
        pl.setId(e.getId());
        pl.setCompanyId(e.getCompanyId());
        pl.setName(e.getName());
        pl.setCurrencyCode(e.getCurrencyCode());
        pl.setActive(e.isActive());
        pl.setSequence(e.getSequence());
        pl.setCreatedAt(e.getCreatedAt());
        pl.setUpdatedAt(e.getUpdatedAt());
        List<PricelistItem> items = new ArrayList<>();
        if (e.getItems() != null) {
            for (SalPricelistItemEntity itemEntity : e.getItems()) {
                items.add(pricelistItemEntityToDomain(itemEntity));
            }
        }
        pl.setItems(items);
        return pl;
    }

    private PricelistItem pricelistItemEntityToDomain(SalPricelistItemEntity e) {
        PricelistItem item = new PricelistItem();
        item.setId(e.getId());
        item.setSequence(e.getSequence());
        item.setProductId(e.getProductId());
        item.setMinQuantity(e.getMinQuantity());
        item.setFixedPrice(e.getFixedPrice());
        item.setPercentDiscount(e.getPercentDiscount());
        item.setDateFrom(e.getDateFrom());
        item.setDateTo(e.getDateTo());
        item.setCreatedAt(e.getCreatedAt());
        item.setUpdatedAt(e.getUpdatedAt());
        return item;
    }
}
