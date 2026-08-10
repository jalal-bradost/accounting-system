package com.jalaldeveloper.accountingsystem.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceLineEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerInvoiceLineTaxEntity;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.CustomerInvoiceMoveType;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoice;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoiceLine;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerInvoiceLineTax;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomerInvoiceDataAccessMapper {

    public CustomerInvoice entityToDomain(AccCustomerInvoiceEntity entity) {
        if (entity == null) return null;
        CustomerInvoice domain = new CustomerInvoice();
        domain.setId(entity.getId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setCustomerPartnerId(entity.getCustomerPartnerId());
        domain.setInvoiceDate(entity.getInvoiceDate());
        domain.setDueDate(entity.getDueDate());
        domain.setReference(entity.getReference());
        domain.setCurrencyCode(entity.getCurrencyCode());
        domain.setState(entity.getState());
        domain.setMoveType(entity.getMoveType() != null ? entity.getMoveType() : CustomerInvoiceMoveType.INVOICE);
        domain.setReversedInvoiceId(entity.getReversedInvoiceId());
        domain.setJournalEntryId(entity.getJournalEntryId());
        domain.setSalesOrderId(entity.getSalesOrderId());
        domain.setExchangeRateToCompany(entity.getExchangeRateToCompany());
        domain.setRowVersion(entity.getRowVersion());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        entity.getLines().size();
        List<CustomerInvoiceLine> lines = new ArrayList<>();
        for (AccCustomerInvoiceLineEntity lineEntity : entity.getLines()) {
            lines.add(lineEntityToDomain(lineEntity));
        }
        domain.setLines(lines);
        return domain;
    }

    private CustomerInvoiceLine lineEntityToDomain(AccCustomerInvoiceLineEntity entity) {
        CustomerInvoiceLine line = new CustomerInvoiceLine();
        line.setId(entity.getId());
        line.setSequence(entity.getSequence());
        line.setName(entity.getName());
        line.setQty(entity.getQty());
        line.setUnitPrice(entity.getUnitPrice());
        line.setDiscountPercent(entity.getDiscountPercent());
        line.setRevenueAccountId(entity.getRevenueAccountId());
        line.setSalesOrderLineId(entity.getSalesOrderLineId());
        line.setCreatedAt(entity.getCreatedAt());
        line.setUpdatedAt(entity.getUpdatedAt());
        entity.getTaxSnapshots().size();
        List<CustomerInvoiceLineTax> taxes = new ArrayList<>();
        for (AccCustomerInvoiceLineTaxEntity taxEntity : entity.getTaxSnapshots()) {
            taxes.add(taxEntityToDomain(taxEntity));
        }
        line.setTaxSnapshots(taxes);
        return line;
    }

    private CustomerInvoiceLineTax taxEntityToDomain(AccCustomerInvoiceLineTaxEntity entity) {
        CustomerInvoiceLineTax tax = new CustomerInvoiceLineTax();
        tax.setId(entity.getId());
        tax.setTaxId(entity.getTaxId());
        tax.setTaxName(entity.getTaxName());
        tax.setTaxBase(entity.getTaxBase());
        tax.setTaxAmount(entity.getTaxAmount());
        tax.setAccountId(entity.getAccountId());
        return tax;
    }

    public AccCustomerInvoiceEntity domainToEntity(CustomerInvoice domain, AccCustomerInvoiceEntity existing) {
        if (domain == null) return null;
        AccCustomerInvoiceEntity entity = existing != null ? existing : new AccCustomerInvoiceEntity();
        entity.setId(domain.getId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setCustomerPartnerId(domain.getCustomerPartnerId());
        entity.setInvoiceDate(domain.getInvoiceDate());
        entity.setDueDate(domain.getDueDate());
        entity.setReference(domain.getReference());
        entity.setCurrencyCode(domain.getCurrencyCode());
        entity.setState(domain.getState());
        entity.setMoveType(domain.getMoveType());
        entity.setReversedInvoiceId(domain.getReversedInvoiceId());
        entity.setJournalEntryId(domain.getJournalEntryId());
        entity.setSalesOrderId(domain.getSalesOrderId());
        entity.setExchangeRateToCompany(domain.getExchangeRateToCompany());
        if (existing == null) {
            entity.setRowVersion(domain.getRowVersion());
            entity.setCreatedAt(domain.getCreatedAt());
        }
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (existing == null) {
            entity.getLines().clear();
            for (CustomerInvoiceLine lineDomain : domain.getLines()) {
                entity.getLines().add(lineDomainToEntity(lineDomain, entity));
            }
        } else {
            mergeLines(entity, domain.getLines());
        }
        return entity;
    }

    private void mergeLines(AccCustomerInvoiceEntity invoiceEntity, List<CustomerInvoiceLine> lineDomains) {
        invoiceEntity.getLines().clear();
        for (CustomerInvoiceLine lineDomain : lineDomains) {
            invoiceEntity.getLines().add(lineDomainToEntity(lineDomain, invoiceEntity));
        }
    }

    private AccCustomerInvoiceLineEntity lineDomainToEntity(CustomerInvoiceLine domain, AccCustomerInvoiceEntity invoice) {
        AccCustomerInvoiceLineEntity entity = new AccCustomerInvoiceLineEntity();
        entity.setId(domain.getId());
        entity.setInvoice(invoice);
        entity.setSequence(domain.getSequence());
        entity.setName(domain.getName());
        entity.setQty(domain.getQty());
        entity.setUnitPrice(domain.getUnitPrice());
        entity.setDiscountPercent(domain.getDiscountPercent());
        entity.setRevenueAccountId(domain.getRevenueAccountId());
        entity.setSalesOrderLineId(domain.getSalesOrderLineId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        for (CustomerInvoiceLineTax taxDomain : domain.getTaxSnapshots()) {
            AccCustomerInvoiceLineTaxEntity taxEntity = new AccCustomerInvoiceLineTaxEntity();
            taxEntity.setId(taxDomain.getId());
            taxEntity.setLine(entity);
            taxEntity.setTaxId(taxDomain.getTaxId());
            taxEntity.setTaxName(taxDomain.getTaxName());
            taxEntity.setTaxBase(taxDomain.getTaxBase());
            taxEntity.setTaxAmount(taxDomain.getTaxAmount());
            taxEntity.setAccountId(taxDomain.getAccountId());
            entity.getTaxSnapshots().add(taxEntity);
        }
        return entity;
    }
}
