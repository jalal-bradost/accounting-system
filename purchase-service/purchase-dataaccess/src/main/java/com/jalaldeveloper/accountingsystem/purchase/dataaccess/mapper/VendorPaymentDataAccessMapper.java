package com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorPaymentEntity;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorPayment;
import org.springframework.stereotype.Component;

@Component
public class VendorPaymentDataAccessMapper {

    public VendorPayment entityToDomain(PurVendorPaymentEntity e) {
        if (e == null) return null;
        VendorPayment d = new VendorPayment();
        d.setId(e.getId());
        d.setCompanyId(e.getCompanyId());
        d.setVendorPartnerId(e.getVendorPartnerId());
        d.setVendorBillId(e.getVendorBillId());
        d.setPaymentDate(e.getPaymentDate());
        d.setBankJournalId(e.getBankJournalId());
        d.setAmount(e.getAmount());
        d.setCurrencyCode(e.getCurrencyCode());
        d.setExchangeRateToCompany(e.getExchangeRateToCompany());
        d.setState(e.getState());
        d.setJournalEntryId(e.getJournalEntryId());
        d.setReference(e.getReference());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public PurVendorPaymentEntity domainToEntity(VendorPayment d, PurVendorPaymentEntity existingOrNull) {
        if (d == null) return null;
        PurVendorPaymentEntity e = existingOrNull != null ? existingOrNull : new PurVendorPaymentEntity();
        e.setId(d.getId());
        e.setCompanyId(d.getCompanyId());
        e.setVendorPartnerId(d.getVendorPartnerId());
        e.setVendorBillId(d.getVendorBillId());
        e.setPaymentDate(d.getPaymentDate());
        e.setBankJournalId(d.getBankJournalId());
        e.setAmount(d.getAmount());
        e.setCurrencyCode(d.getCurrencyCode());
        e.setExchangeRateToCompany(d.getExchangeRateToCompany());
        e.setState(d.getState());
        e.setJournalEntryId(d.getJournalEntryId());
        e.setReference(d.getReference());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }
}
