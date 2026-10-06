package com.bradox.erp.purchase.dataaccess.adapter;

import com.bradox.erp.purchase.dataaccess.entity.PurVendorPaymentAllocationEntity;
import com.bradox.erp.purchase.dataaccess.repository.PurVendorPaymentAllocationJpaRepository;
import com.bradox.erp.purchase.domain.core.entity.VendorPaymentAllocation;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorPaymentAllocationRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VendorPaymentAllocationRepositoryImpl implements VendorPaymentAllocationRepository {

    private final PurVendorPaymentAllocationJpaRepository jpa;

    public VendorPaymentAllocationRepositoryImpl(PurVendorPaymentAllocationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public VendorPaymentAllocation save(VendorPaymentAllocation a) {
        PurVendorPaymentAllocationEntity e = jpa.findById(a.getId()).orElseGet(PurVendorPaymentAllocationEntity::new);
        e.setId(a.getId());
        e.setCompanyId(a.getCompanyId());
        e.setPaymentId(a.getPaymentId());
        e.setVendorBillId(a.getVendorBillId());
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
    public Optional<VendorPaymentAllocation> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<VendorPaymentAllocation> findByPaymentIdIn(Collection<UUID> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) return List.of();
        return jpa.findByPaymentIdInOrderByCreatedAtAsc(paymentIds).stream().map(this::toDomain).toList();
    }

    @Override
    public List<VendorPaymentAllocation> findByBillIdIn(Collection<UUID> billIds) {
        if (billIds == null || billIds.isEmpty()) return List.of();
        return jpa.findByVendorBillIdInOrderByCreatedAtAsc(billIds).stream().map(this::toDomain).toList();
    }

    @Override
    public List<VendorPaymentAllocation> findByCompanyId(UUID companyId) {
        return jpa.findByCompanyId(companyId).stream().map(this::toDomain).toList();
    }

    private VendorPaymentAllocation toDomain(PurVendorPaymentAllocationEntity e) {
        VendorPaymentAllocation a = new VendorPaymentAllocation();
        a.setId(e.getId());
        a.setCompanyId(e.getCompanyId());
        a.setPaymentId(e.getPaymentId());
        a.setVendorBillId(e.getVendorBillId());
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
