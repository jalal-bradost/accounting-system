package com.bradox.erp.dataaccess.adapter;

import com.bradox.erp.accounting.service.domain.ports.output.repository.CustomerPaymentAllocationRepository;
import com.bradox.erp.dataaccess.entity.AccCustomerPaymentAllocationEntity;
import com.bradox.erp.dataaccess.repository.AccCustomerPaymentAllocationJpaRepository;
import com.bradox.erp.domain.core.entity.CustomerPaymentAllocation;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerPaymentAllocationRepositoryImpl implements CustomerPaymentAllocationRepository {

    private final AccCustomerPaymentAllocationJpaRepository jpa;

    public CustomerPaymentAllocationRepositoryImpl(AccCustomerPaymentAllocationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CustomerPaymentAllocation save(CustomerPaymentAllocation a) {
        AccCustomerPaymentAllocationEntity e = jpa.findById(a.getId()).orElseGet(AccCustomerPaymentAllocationEntity::new);
        e.setId(a.getId());
        e.setCompanyId(a.getCompanyId());
        e.setPaymentId(a.getPaymentId());
        e.setCustomerInvoiceId(a.getCustomerInvoiceId());
        e.setAmount(a.getAmount());
        e.setPaymentAmount(a.getPaymentAmount());
        e.setAmountCompany(a.getAmountCompany());
        e.setPaymentAmountCompany(a.getPaymentAmountCompany());
        e.setAllocationDate(a.getAllocationDate());
        e.setFxJournalEntryId(a.getFxJournalEntryId());
        e.setFxReversalJournalEntryId(a.getFxReversalJournalEntryId());
        e.setState(a.getState());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return toDomain(jpa.save(e));
    }

    @Override
    public Optional<CustomerPaymentAllocation> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<CustomerPaymentAllocation> findByPaymentIdIn(Collection<UUID> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) return List.of();
        return jpa.findByPaymentIdInOrderByCreatedAtAsc(paymentIds).stream().map(this::toDomain).toList();
    }

    @Override
    public List<CustomerPaymentAllocation> findByInvoiceIdIn(Collection<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) return List.of();
        return jpa.findByCustomerInvoiceIdInOrderByCreatedAtAsc(invoiceIds).stream().map(this::toDomain).toList();
    }

    @Override
    public List<CustomerPaymentAllocation> findByCompanyId(UUID companyId) {
        return jpa.findByCompanyId(companyId).stream().map(this::toDomain).toList();
    }

    private CustomerPaymentAllocation toDomain(AccCustomerPaymentAllocationEntity e) {
        CustomerPaymentAllocation a = new CustomerPaymentAllocation();
        a.setId(e.getId());
        a.setCompanyId(e.getCompanyId());
        a.setPaymentId(e.getPaymentId());
        a.setCustomerInvoiceId(e.getCustomerInvoiceId());
        a.setAmount(e.getAmount());
        a.setPaymentAmount(e.getPaymentAmount());
        a.setAmountCompany(e.getAmountCompany());
        a.setPaymentAmountCompany(e.getPaymentAmountCompany());
        a.setAllocationDate(e.getAllocationDate());
        a.setFxJournalEntryId(e.getFxJournalEntryId());
        a.setFxReversalJournalEntryId(e.getFxReversalJournalEntryId());
        a.setState(e.getState());
        a.setCreatedAt(e.getCreatedAt());
        a.setUpdatedAt(e.getUpdatedAt());
        return a;
    }
}
