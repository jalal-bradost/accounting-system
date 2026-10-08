package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.model.PackageLine;
import com.bradox.erp.repair.domain.core.model.ServicePackage;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.service.domain.dto.PackageCommand;
import com.bradox.erp.repair.service.domain.ports.input.PackageApplicationService;
import com.bradox.erp.repair.service.domain.ports.output.repository.PackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class PackageApplicationServiceImpl implements PackageApplicationService {

    private final PackageRepository packages;
    private final RepairAccess access;
    private final AuditLogPort audit;

    PackageApplicationServiceImpl(PackageRepository packages, RepairAccess access, AuditLogPort audit) {
        this.packages = packages;
        this.access = access;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicePackage> list(CompanyId companyId, String query, boolean includeArchived) {
        access.require(RepairPermissions.ORDER_VIEW);
        return packages.list(companyId, query == null || query.isBlank() ? null : query.trim(), includeArchived);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePackage get(CompanyId companyId, UUID id) {
        access.require(RepairPermissions.ORDER_VIEW);
        return packages.find(companyId, id).orElseThrow(() -> RepairSupport.notFound("Package"));
    }

    @Override
    @Transactional
    public ServicePackage save(CompanyId companyId, UUID id, PackageCommand c) {
        access.require(RepairPermissions.PACKAGE_MANAGE);
        ServicePackage existing = id == null ? null : packages.find(companyId, id).orElseThrow(() -> RepairSupport.notFound("Package"));
        List<PackageLine> lines = new ArrayList<>();
        int seq = 0;
        for (PackageCommand.Line l : c.lines() == null ? List.<PackageCommand.Line>of() : c.lines()) {
            lines.add(toLine(l, seq++));
        }
        if (lines.isEmpty()) {
            throw new RepairDomainException("error.repair.package.empty", null, "A package needs at least one line");
        }
        ServicePackage saved = packages.save(new ServicePackage(existing != null ? existing.id() : UUID.randomUUID(), companyId.getId(),
                c.code() == null || c.code().isBlank() ? null : c.code().trim(), c.name().trim(), c.category(), c.active(),
                existing != null ? existing.createdAt() : access.now(), existing != null ? existing.createdBy() : access.actorLabel(),
                lines));
        audit.recordBusinessEvent(companyId, "rep.package", saved.id(), "Service package saved", Map.of("name", saved.name()));
        return saved;
    }

    @Override
    @Transactional
    public ServicePackage setActive(CompanyId companyId, UUID id, boolean active) {
        access.require(RepairPermissions.PACKAGE_MANAGE);
        ServicePackage p = packages.find(companyId, id).orElseThrow(() -> RepairSupport.notFound("Package"));
        ServicePackage saved = packages.save(new ServicePackage(p.id(), p.companyId(), p.code(), p.name(), p.category(), active,
                p.createdAt(), p.createdBy(), p.lines()));
        audit.recordBusinessEvent(companyId, "rep.package", id, active ? "Package restored" : "Package archived", Map.of());
        return saved;
    }

    private static PackageLine toLine(PackageCommand.Line l, int seq) {
        if (l.type() == null || l.type() == LineType.REMOVED_PART || l.type() == LineType.CUSTOMER_PART) {
            throw new RepairDomainException("error.repair.package.type", null, "A package line is a part, an operation or a sublet");
        }
        BigDecimal qty = l.qty() == null ? BigDecimal.ONE : l.qty();
        if (qty.signum() <= 0) {
            throw new RepairDomainException("error.repair.line.qty", null, "Quantity must be greater than zero");
        }
        if (l.type() == LineType.PART && l.productId() == null && (l.description() == null || l.description().isBlank())) {
            throw new RepairDomainException("error.repair.line.description", null, "A line needs a description");
        }
        if (l.type() == LineType.OPERATION && l.laborGuideId() == null && (l.standardMinutes() == null || l.standardMinutes() <= 0)) {
            throw new RepairDomainException("error.repair.package.minutes", null,
                    "An operation needs a labor-guide entry or its own standard minutes");
        }
        return new PackageLine(UUID.randomUUID(), seq, l.type(), l.productId(), l.laborGuideId(), l.description(), qty, l.standardMinutes());
    }
}
