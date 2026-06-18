package com.jalaldeveloper.accountingsystem.purchase.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurVendorPaymentEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper.VendorPaymentDataAccessMapper;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository.PurVendorPaymentJpaRepository;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.VendorPayment;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.VendorPaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class VendorPaymentRepositoryImpl implements VendorPaymentRepository {

    private final PurVendorPaymentJpaRepository jpa;
    private final VendorPaymentDataAccessMapper mapper;

    public VendorPaymentRepositoryImpl(PurVendorPaymentJpaRepository jpa, VendorPaymentDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public VendorPayment save(VendorPayment payment) {
        PurVendorPaymentEntity existing = jpa.findById(payment.getId()).orElse(null);
        PurVendorPaymentEntity toSave = mapper.domainToEntity(payment, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<VendorPayment> findById(UUID id) {
        return jpa.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public List<VendorPayment> findByVendorBillId(UUID vendorBillId) {
        return jpa.findByVendorBillId(vendorBillId).stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId) {
        return jpa.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId).stream()
                .map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorPayment> findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(UUID companyId,
                                                                                                 UUID vendorPartnerId) {
        return jpa.findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(companyId, vendorPartnerId)
                .stream().map(mapper::entityToDomain).toList();
    }
}
