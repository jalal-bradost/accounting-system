package com.bradox.erp.purchase.dataaccess.adapter;

import com.bradox.erp.purchase.dataaccess.entity.PurPurchaseOrderEntity;
import com.bradox.erp.purchase.dataaccess.mapper.PurchaseOrderDataAccessMapper;
import com.bradox.erp.purchase.dataaccess.repository.PurPurchaseOrderJpaRepository;
import com.bradox.erp.purchase.domain.core.PurchaseOrderState;
import com.bradox.erp.purchase.domain.core.entity.PurchaseOrder;
import com.bradox.erp.purchase.service.domain.ports.output.repository.PurchaseOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
        PurPurchaseOrderEntity existing = order.getId() != null
                ? entityManager.find(PurPurchaseOrderEntity.class, order.getId())
                : null;
        if (existing == null) {
            // Assigned UUID + primitive @Version makes Spring Data treat the entity as not-new
            // (merge instead of persist). Persist explicitly so create → flush → find works.
            PurPurchaseOrderEntity toSave = mapper.domainToEntity(order, null);
            entityManager.persist(toSave);
            return mapper.entityToDomain(toSave);
        }
        entityManager.lock(existing, LockModeType.PESSIMISTIC_WRITE);
        PurPurchaseOrderEntity toSave = mapper.domainToEntity(order, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<PurchaseOrder> findById(UUID id) {
        PurPurchaseOrderEntity entity = entityManager.find(PurPurchaseOrderEntity.class, id);
        if (entity == null) {
            return Optional.empty();
        }
        touchLines(entity);
        return Optional.of(mapper.entityToDomain(entity));
    }

    @Override
    public Optional<PurchaseOrder> findByIdForUpdate(UUID id) {
        PurPurchaseOrderEntity entity = entityManager.find(
                PurPurchaseOrderEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            return Optional.empty();
        }
        touchLines(entity);
        return Optional.of(mapper.entityToDomain(entity));
    }

    private static void touchLines(PurPurchaseOrderEntity entity) {
        if (entity.getLines() != null) {
            for (var line : entity.getLines()) {
                if (line.getTaxes() != null) {
                    line.getTaxes().size();
                }
            }
        }
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
