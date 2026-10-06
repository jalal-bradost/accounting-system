package com.bradox.erp.purchase.dataaccess.adapter;

import com.bradox.erp.purchase.dataaccess.entity.PurVendorBillEntity;
import com.bradox.erp.purchase.dataaccess.mapper.VendorBillDataAccessMapper;
import com.bradox.erp.purchase.dataaccess.repository.PurVendorBillJpaRepository;
import com.bradox.erp.purchase.domain.core.entity.VendorBill;
import com.bradox.erp.purchase.service.domain.ports.output.repository.VendorBillRepository;
import com.bradox.erp.purchase.domain.core.VendorBillState;
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
    public List<VendorBill> findByPurchaseOrderIdIn(Collection<UUID> purchaseOrderIds) {
        if (purchaseOrderIds == null || purchaseOrderIds.isEmpty()) return List.of();
        return jpa.findByPurchaseOrderIdIn(purchaseOrderIds).stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public Page<VendorBill> searchByCompanyId(UUID companyId, Pageable pageable) {
        Page<UUID> idPage = jpa.findIdsByCompanyId(companyId, pageable);
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        Map<UUID, VendorBill> byId = jpa.findByIdIn(idPage.getContent()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toMap(VendorBill::getId, Function.identity(), (a, b) -> a));
        List<VendorBill> ordered = new ArrayList<>();
        for (UUID id : idPage.getContent()) {
            VendorBill b = byId.get(id);
            if (b != null) ordered.add(b);
        }
        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Override
    public List<VendorBill> findPostedByPartnerBefore(UUID companyId, UUID partnerId, LocalDate before) {
        return jpa.findPostedByPartnerBefore(companyId, partnerId, VendorBillState.POSTED, before).stream()
                .map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorBill> findPostedByPartnerBetween(UUID companyId, UUID partnerId, LocalDate from, LocalDate to) {
        return jpa.findPostedByPartnerBetween(companyId, partnerId, VendorBillState.POSTED, from, to).stream()
                .map(mapper::entityToDomain).toList();
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

    @Override
    public List<VendorBill> findByReversedBillId(UUID reversedBillId) {
        return jpa.findByReversedBillIdOrderByBillDateDescCreatedAtDesc(reversedBillId).stream()
                .map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorBill> findByReversedBillIdIn(java.util.Collection<UUID> reversedBillIds) {
        if (reversedBillIds == null || reversedBillIds.isEmpty()) return List.of();
        return jpa.findByReversedBillIdIn(reversedBillIds).stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public List<VendorBill> findByIdIn(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jpa.findByIdIn(ids).stream().map(mapper::entityToDomain).toList();
    }

    @Override
    public void lockById(UUID id) {
        jpa.lockById(id);
    }

    @Override
    public List<VendorBill> findOpeningBalanceByCompanyId(UUID companyId) {
        return jpa.findByCompanyIdAndOpeningBalanceTrueOrderByBillDateAscCreatedAtAsc(companyId).stream()
                .map(mapper::entityToDomain).toList();
    }
}
