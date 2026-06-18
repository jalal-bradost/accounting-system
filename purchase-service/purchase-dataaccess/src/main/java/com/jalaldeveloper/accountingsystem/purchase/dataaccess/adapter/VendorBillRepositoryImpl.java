package com.jalaldeveloper.accountingsystem.purchase.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorBillEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper.VendorBillDataAccessMapper;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository.PurVendorBillJpaRepository;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorBill;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.VendorBillRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VendorBillRepositoryImpl implements VendorBillRepository {

    private final PurVendorBillJpaRepository jpa;
    private final VendorBillDataAccessMapper mapper;

    public VendorBillRepositoryImpl(PurVendorBillJpaRepository jpa, VendorBillDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public VendorBill save(VendorBill bill) {
        PurVendorBillEntity existing = jpa.findById(bill.getId()).orElse(null);
        PurVendorBillEntity toSave = mapper.domainToEntity(bill, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<VendorBill> findById(UUID id) {
        return jpa.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public List<VendorBill> findByPurchaseOrderId(UUID purchaseOrderId) {
        return jpa.findByPurchaseOrderId(purchaseOrderId).stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorBill> findByCompanyIdOrderByBillDateDescCreatedAtDesc(UUID companyId) {
        return jpa.findByCompanyIdOrderByBillDateDescCreatedAtDesc(companyId).stream()
                .map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorBill> findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(UUID companyId,
                                                                                            UUID vendorPartnerId) {
        return jpa.findByCompanyIdAndVendorPartnerIdOrderByBillDateAscCreatedAtAsc(companyId, vendorPartnerId).stream()
                .map(mapper::entityToDomain).toList();
    }
}
