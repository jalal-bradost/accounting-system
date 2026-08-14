package com.jalaldeveloper.accountingsystem.sales.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.sales.dataaccess.entity.SalSalesOrderEntity;
import com.jalaldeveloper.accountingsystem.sales.dataaccess.mapper.SalesOrderDataAccessMapper;
import com.jalaldeveloper.accountingsystem.sales.dataaccess.repository.SalSalesOrderJpaRepository;
import com.jalaldeveloper.accountingsystem.sales.domain.core.SalesOrderState;
import com.jalaldeveloper.accountingsystem.sales.domain.core.entity.SalesOrder;
import com.jalaldeveloper.accountingsystem.sales.service.domain.ports.output.repository.SalesOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SalesOrderRepositoryImpl implements SalesOrderRepository {

    private final SalSalesOrderJpaRepository jpa;
    private final SalesOrderDataAccessMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public SalesOrderRepositoryImpl(SalSalesOrderJpaRepository jpa, SalesOrderDataAccessMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public SalesOrder save(SalesOrder order) {
        SalSalesOrderEntity existing = null;
        if (order.getId() != null) {
            existing = entityManager.find(SalSalesOrderEntity.class, order.getId());
            if (existing != null) {
                entityManager.detach(existing);
                existing = entityManager.find(
                        SalSalesOrderEntity.class, order.getId(), LockModeType.PESSIMISTIC_WRITE);
            }
        }
        SalSalesOrderEntity toSave = mapper.domainToEntity(order, existing);
        return mapper.entityToDomain(jpa.save(toSave));
    }

    @Override
    public Optional<SalesOrder> findByIdWithLines(UUID id) {
        return jpa.findByIdWithLines(id).map(mapper::entityToDomain);
    }

    @Override
    public Optional<SalesOrder> findByIdForUpdate(UUID id) {
        SalSalesOrderEntity cached = entityManager.find(SalSalesOrderEntity.class, id);
        if (cached != null) {
            entityManager.detach(cached);
        }
        SalSalesOrderEntity entity = entityManager.find(
                SalSalesOrderEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            return Optional.empty();
        }
        if (entity.getLines() != null) {
            for (var line : entity.getLines()) {
                if (line.getTaxes() != null) {
                    line.getTaxes().size();
                }
            }
        }
        return Optional.of(mapper.entityToDomain(entity));
    }

    @Override
    public Optional<SalesOrder> findByCompanyIdAndName(UUID companyId, String name) {
        return jpa.findByCompanyIdAndName(companyId, name).map(mapper::entityToDomain);
    }

    @Override
    public Page<SalesOrder> search(UUID companyId,
                                   SalesOrderState state,
                                   UUID customerPartnerId,
                                   String q,
                                   Pageable pageable) {
        return jpa.search(companyId, state, customerPartnerId, q, pageable).map(mapper::entityToDomain);
    }

    @Override
    public void flush() {
        entityManager.flush();
    }
}
