package com.jalaldeveloper.accountingsystem.purchase.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.purchase.dataaccess.entity.PurPurchaseOrderEntity;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.mapper.PurchaseOrderDataAccessMapper;
import com.jalaldeveloper.accountingsystem.purchase.dataaccess.repository.PurPurchaseOrderJpaRepository;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseOrderState;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.entity.PurchaseOrder;
import com.jalaldeveloper.accountingsystem.purchase.service.domain.ports.output.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepository {

    private final PurPurchaseOrderJpaRepository jpa;
    private final PurchaseOrderDataAccessMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public PurchaseOrderRepositoryImpl(PurPurchaseOrderJpaRepository jpa, PurchaseOrderDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PurchaseOrder save(PurchaseOrder order) {
        PurPurchaseOrderEntity existing = jpa.findById(order.getId()).orElse(null);
        PurPurchaseOrderEntity toSave = mapper.domainToEntity(order, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<PurchaseOrder> findById(UUID id) {
        return jpa.findById(id).map(mapper::entityToDomain);
    }

    @Override
    public Optional<PurchaseOrder> findByCompanyIdAndName(UUID companyId, String name) {
        return jpa.findByCompanyIdAndName(companyId, name).map(mapper::entityToDomain);
    }

    @Override
    public Page<PurchaseOrder> search(UUID companyId,
                                      PurchaseOrderState state,
                                      UUID vendorPartnerId,
                                      String q,
                                      Pageable pageable) {
        return jpa.search(companyId, state, vendorPartnerId, q, pageable).map(mapper::entityToDomain);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }
}
