package com.bradox.erp.repair.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.dataaccess.entity.LineEntity;
import com.bradox.erp.repair.dataaccess.repository.LineJpaRepository;
import com.bradox.erp.repair.domain.core.model.RepairLine;
import com.bradox.erp.repair.domain.core.valueobject.Disposition;
import com.bradox.erp.repair.domain.core.valueobject.LineStatus;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.domain.core.valueobject.PricingMode;
import com.bradox.erp.repair.domain.core.valueobject.SubletStatus;
import com.bradox.erp.repair.service.domain.ports.output.repository.LineRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LineRepositoryImpl implements LineRepository {

    private final LineJpaRepository jpa;

    public LineRepositoryImpl(LineJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<RepairLine> listByOrder(CompanyId companyId, UUID orderId) {
        return jpa.findByCompanyIdAndOrderId(companyId.getId(), orderId).stream().map(LineRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<RepairLine> find(CompanyId companyId, UUID id) {
        return jpa.findByCompanyIdAndId(companyId.getId(), id).map(LineRepositoryImpl::toDomain);
    }

    @Override
    public RepairLine save(RepairLine l) {
        LineEntity e = jpa.findById(l.id()).orElseGet(LineEntity::new);
        e.id = l.id();
        e.companyId = l.companyId();
        e.orderId = l.orderId();
        e.lineType = l.type().name();
        e.sectionLabel = l.sectionLabel();
        e.sequence = l.sequence();
        e.productId = l.productId();
        e.description = l.description();
        e.qty = l.qty();
        e.unitPrice = l.unitPrice();
        e.discountPercent = l.discountPercent();
        e.status = l.status().name();
        e.needsDiscountApproval = l.needsDiscountApproval();
        e.fromFindingId = l.fromFindingId();
        e.fromPackageId = l.fromPackageId();
        e.laborGuideId = l.laborGuideId();
        e.pricingMode = l.pricingMode() == null ? null : l.pricingMode().name();
        e.standardMinutes = l.standardMinutes();
        e.laborRate = l.laborRate();
        e.assigneeEmployeeId = l.assigneeEmployeeId();
        e.vendorPartnerId = l.vendorPartnerId();
        e.vendorCost = l.vendorCost();
        e.subletStatus = l.subletStatus() == null ? null : l.subletStatus().name();
        e.disposition = l.disposition() == null ? null : l.disposition().name();
        e.createdAt = l.createdAt();
        e.createdBy = l.createdBy();
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public void delete(CompanyId companyId, UUID id) {
        jpa.findByCompanyIdAndId(companyId.getId(), id).ifPresent(jpa::delete);
    }

    private static RepairLine toDomain(LineEntity e) {
        return new RepairLine(e.id, e.companyId, e.orderId, LineType.valueOf(e.lineType), e.sectionLabel, e.sequence, e.productId,
                e.description, e.qty, e.unitPrice, e.discountPercent, LineStatus.valueOf(e.status), e.needsDiscountApproval,
                e.fromFindingId, e.fromPackageId, e.laborGuideId, e.pricingMode == null ? null : PricingMode.valueOf(e.pricingMode),
                e.standardMinutes, e.laborRate, e.assigneeEmployeeId, e.vendorPartnerId, e.vendorCost,
                e.subletStatus == null ? null : SubletStatus.valueOf(e.subletStatus),
                e.disposition == null ? null : Disposition.valueOf(e.disposition), e.createdAt, e.createdBy);
    }
}
