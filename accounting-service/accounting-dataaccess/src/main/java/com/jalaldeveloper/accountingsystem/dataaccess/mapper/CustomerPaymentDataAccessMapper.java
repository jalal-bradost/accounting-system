package com.jalaldeveloper.accountingsystem.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccCustomerPaymentEntity;
import com.jalaldeveloper.accountingsystem.domain.core.entity.CustomerPayment;
import org.springframework.stereotype.Component;

@Component
public class CustomerPaymentDataAccessMapper {

    public CustomerPayment entityToDomain(AccCustomerPaymentEntity entity) {
        if (entity == null) return null;
        CustomerPayment domain = new CustomerPayment();
        domain.setId(entity.getId());
        domain.setCompanyId(entity.getCompanyId());
        domain.setCustomerPartnerId(entity.getCustomerPartnerId());
        domain.setCustomerInvoiceId(entity.getCustomerInvoiceId());
        domain.setPaymentDate(entity.getPaymentDate());
        domain.setPaymentJournalId(entity.getPaymentJournalId());
        domain.setAmount(entity.getAmount());
        domain.setCurrencyCode(entity.getCurrencyCode());
        domain.setExchangeRateToCompany(entity.getExchangeRateToCompany());
        domain.setJournalEntryId(entity.getJournalEntryId());
        domain.setReference(entity.getReference());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public AccCustomerPaymentEntity domainToEntity(CustomerPayment domain) {
        if (domain == null) return null;
        AccCustomerPaymentEntity entity = new AccCustomerPaymentEntity();
        entity.setId(domain.getId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setCustomerPartnerId(domain.getCustomerPartnerId());
        entity.setCustomerInvoiceId(domain.getCustomerInvoiceId());
        entity.setPaymentDate(domain.getPaymentDate());
        entity.setPaymentJournalId(domain.getPaymentJournalId());
        entity.setAmount(domain.getAmount());
        entity.setCurrencyCode(domain.getCurrencyCode());
        entity.setExchangeRateToCompany(domain.getExchangeRateToCompany());
        entity.setJournalEntryId(domain.getJournalEntryId());
        entity.setReference(domain.getReference());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
