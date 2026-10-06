package com.bradox.erp.purchase.dataaccess.adapter;

import com.bradox.erp.purchase.dataaccess.entity.PurVendorPaymentEntity;
import com.bradox.erp.purchase.dataaccess.mapper.VendorPaymentDataAccessMapper;
import com.bradox.erp.purchase.dataaccess.repository.PurVendorPaymentJpaRepository;
import com.bradox.erp.purchase.domain.core.entity.VendorPayment;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorPaymentRepository;
import com.bradox.erp.purchase.domain.core.VendorPaymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public Optional<VendorPayment> findByIdForUpdate(UUID id) {
        return jpa.findByIdForUpdate(id).map(mapper::entityToDomain);
    }

    @Override
    public List<VendorPayment> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jpa.findByIdIn(ids).stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorPayment> findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(UUID companyId) {
        return jpa.findByCompanyIdOrderByPaymentDateDescCreatedAtDesc(companyId).stream()
                .map(mapper::entityToDomain).toList();
    }

    @Override
    public Page<VendorPayment> searchByCompanyId(UUID companyId, Pageable pageable) {
        Page<UUID> idPage = jpa.findIdsByCompanyId(companyId, pageable);
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        Map<UUID, VendorPayment> byId = jpa.findByIdIn(idPage.getContent()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toMap(VendorPayment::getId, Function.identity(), (a, b) -> a));
        List<VendorPayment> ordered = new ArrayList<>();
        for (UUID id : idPage.getContent()) {
            VendorPayment pay = byId.get(id);
            if (pay != null) ordered.add(pay);
        }
        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Override
    public List<VendorPayment> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before) {
        return jpa.findPostedByPartnerBefore(
                        companyId, partnerId, VendorPaymentState.POSTED, before.atStartOfDay())
                .stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorPayment> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to) {
        return jpa.findPostedByPartnerBetween(
                        companyId,
                        partnerId,
                        VendorPaymentState.POSTED,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay())
                .stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorPayment> findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(UUID companyId,
                                                                                                 UUID vendorPartnerId) {
        return jpa.findByCompanyIdAndVendorPartnerIdOrderByPaymentDateAscCreatedAtAsc(companyId, vendorPartnerId)
                .stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorPayment> findOpeningBalanceByCompanyId(UUID companyId) {
        return jpa.findByCompanyIdAndOpeningBalanceTrueOrderByPaymentDateAscCreatedAtAsc(companyId).stream()
                .map(mapper::entityToDomain).toList();
    }
}
