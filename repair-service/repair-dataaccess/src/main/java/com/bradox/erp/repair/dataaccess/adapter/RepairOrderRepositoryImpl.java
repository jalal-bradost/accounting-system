package com.bradox.erp.repair.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.dataaccess.entity.RepairOrderEntity;
import com.bradox.erp.repair.dataaccess.entity.RepairOrderPartEntity;
import com.bradox.erp.repair.dataaccess.repository.RepairOrderJpaRepository;
import com.bradox.erp.repair.domain.core.model.RepairOrder;
import com.bradox.erp.repair.domain.core.valueobject.RepairStatus;
import com.bradox.erp.repair.service.domain.ports.output.RepairOrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RepairOrderRepositoryImpl implements RepairOrderRepository {

    private final RepairOrderJpaRepository jpa;

    public RepairOrderRepositoryImpl(RepairOrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<RepairOrder> findAll(CompanyId companyId) {
        return jpa.findByCompanyIdOrderByCreatedAtDesc(companyId.getId()).stream().map(RepairOrderRepositoryImpl::toModel).toList();
    }

    @Override
    public Optional<RepairOrder> find(CompanyId companyId, UUID id) {
        return jpa.findByCompanyIdAndId(companyId.getId(), id).map(RepairOrderRepositoryImpl::toModel);
    }

    @Override
    public RepairOrder save(RepairOrder o) {
        RepairOrderEntity e = jpa.findById(o.id()).orElseGet(RepairOrderEntity::new);
        e.id = o.id();
        e.companyId = o.companyId();
        e.reference = o.reference();
        e.customerPartnerId = o.customerPartnerId();
        e.productId = o.productId();
        e.scheduledDate = o.scheduledDate();
        e.underWarranty = o.underWarranty();
        e.status = o.status().name();
        e.saleOrderId = o.saleOrderId();
        e.saleOrderName = o.saleOrderName();
        e.createdAt = o.createdAt();
        e.parts.clear();
        int seq = 0;
        for (RepairOrder.Part p : o.parts()) {
            RepairOrderPartEntity pe = new RepairOrderPartEntity();
            pe.id = p.id();
            pe.sequence = seq++;
            pe.productId = p.productId();
            pe.qty = p.qty();
            e.parts.add(pe);
        }
        return toModel(jpa.save(e));
    }

    private static RepairOrder toModel(RepairOrderEntity e) {
        List<RepairOrder.Part> parts = e.parts.stream().map(p -> new RepairOrder.Part(p.id, p.productId, p.qty)).toList();
        return new RepairOrder(e.id, e.companyId, e.reference, e.customerPartnerId, e.productId, e.scheduledDate,
                e.underWarranty, RepairStatus.valueOf(e.status), e.saleOrderId, e.saleOrderName, parts, e.createdAt);
    }
}
