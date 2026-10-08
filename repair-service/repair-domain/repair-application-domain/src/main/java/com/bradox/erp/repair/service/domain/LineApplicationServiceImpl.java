package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.model.RepairLine;
import com.bradox.erp.repair.domain.core.model.ServicePackage;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.domain.core.rule.LaborPricing;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.domain.core.valueobject.SubletStatus;
import com.bradox.erp.repair.service.domain.dto.AddLineCommand;
import com.bradox.erp.repair.service.domain.dto.AddPackageCommand;
import com.bradox.erp.repair.service.domain.dto.DispositionCommand;
import com.bradox.erp.repair.service.domain.dto.LineResponse;
import com.bradox.erp.repair.service.domain.dto.LinesResponse;
import com.bradox.erp.repair.service.domain.dto.UpdateLineCommand;
import com.bradox.erp.repair.service.domain.ports.input.LineApplicationService;
import com.bradox.erp.repair.service.domain.ports.output.repository.LineRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.PackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class LineApplicationServiceImpl implements LineApplicationService {

    private static final String AUDIT_MODEL = "rep.line";

    private final LineRepository lines;
    private final PackageRepository packages;
    private final LineFactory factory;
    private final RepairSupport support;
    private final RepairAccess access;
    private final AuditLogPort audit;

    LineApplicationServiceImpl(LineRepository lines, PackageRepository packages, LineFactory factory, RepairSupport support,
                               RepairAccess access, AuditLogPort audit) {
        this.lines = lines;
        this.packages = packages;
        this.factory = factory;
        this.support = support;
        this.access = access;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public LinesResponse list(CompanyId companyId, UUID orderId) {
        access.require(RepairPermissions.ORDER_VIEW);
        boolean prices = access.can(RepairPermissions.PRICE_VIEW);
        List<RepairLine> all = lines.listByOrder(companyId, orderId).stream()
                .sorted(Comparator.comparingInt(RepairLine::sequence)).toList();
        BigDecimal total = null;
        if (prices) {
            total = all.stream().filter(l -> l.type() != LineType.CUSTOMER_PART && l.type() != LineType.REMOVED_PART)
                    .map(l -> LaborPricing.lineTotal(l.qty(), l.unitPrice(), l.discountPercent()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return new LinesResponse(all.stream().map(l -> factory.toResponse(l, prices)).toList(), total, prices,
                (int) all.stream().filter(RepairLine::needsDiscountApproval).count());
    }

    @Override
    @Transactional
    public LineResponse add(CompanyId companyId, UUID orderId, AddLineCommand command) {
        access.require(RepairPermissions.LINE_EDIT);
        Settings settings = support.settings(companyId);
        RepairLine line = factory.create(companyId, orderId, command, settings, null, null, null);
        audit(companyId, line, "Line added");
        return factory.toResponse(line, access.can(RepairPermissions.PRICE_VIEW));
    }

    @Override
    @Transactional
    public List<LineResponse> addPackage(CompanyId companyId, UUID orderId, AddPackageCommand command) {
        access.require(RepairPermissions.LINE_EDIT);
        ServicePackage pkg = packages.find(companyId, command.packageId()).orElseThrow(() -> RepairSupport.notFound("Package"));
        if (!pkg.active()) {
            throw new RepairDomainException("error.repair.package.archived", null, "This package is archived and cannot be added");
        }
        boolean prices = access.can(RepairPermissions.PRICE_VIEW);
        List<RepairLine> created = factory.fromPackage(companyId, orderId, pkg, support.settings(companyId), null);
        created.forEach(l -> audit(companyId, l, "Line added from package " + pkg.name()));
        return created.stream().map(l -> factory.toResponse(l, prices)).toList();
    }

    @Override
    @Transactional
    public LineResponse update(CompanyId companyId, UUID lineId, UpdateLineCommand c) {
        access.require(RepairPermissions.LINE_EDIT);
        RepairLine l = load(companyId, lineId);
        requireEditable(l);
        Settings settings = support.settings(companyId);
        boolean noPrice = l.type() == LineType.CUSTOMER_PART || l.type() == LineType.REMOVED_PART;
        BigDecimal discount = noPrice ? BigDecimal.ZERO : c.discountPercent() != null ? c.discountPercent() : l.discountPercent();
        boolean needsApproval = discount.compareTo(settings.advisorDiscountLimitPercent()) > 0
                && !access.can(RepairPermissions.DISCOUNT_APPROVE);
        RepairLine updated = l.with(c.sectionLabel() != null ? c.sectionLabel() : l.sectionLabel(),
                c.sequence() != null ? c.sequence() : l.sequence(), c.description() != null ? c.description() : l.description(),
                c.qty() != null ? c.qty() : l.qty(), noPrice ? BigDecimal.ZERO : c.unitPrice() != null ? c.unitPrice() : l.unitPrice(),
                discount, needsApproval);
        if (l.type() == LineType.SUBLET) {
            updated = updated.withSublet(c.vendorPartnerId() != null ? c.vendorPartnerId() : l.vendorPartnerId(),
                    c.vendorCost() != null ? c.vendorCost() : l.vendorCost(),
                    l.subletStatus() == null ? SubletStatus.REQUESTED : l.subletStatus());
        }
        RepairLine saved = lines.save(updated);
        audit(companyId, saved, "Line edited");
        return factory.toResponse(saved, access.can(RepairPermissions.PRICE_VIEW));
    }

    @Override
    @Transactional
    public void delete(CompanyId companyId, UUID lineId) {
        access.require(RepairPermissions.LINE_EDIT);
        RepairLine l = load(companyId, lineId);
        requireEditable(l);
        lines.delete(companyId, lineId);
        audit(companyId, l, "Line deleted");
    }

    @Override
    @Transactional
    public LineResponse approveDiscount(CompanyId companyId, UUID lineId) {
        access.require(RepairPermissions.DISCOUNT_APPROVE);
        RepairLine saved = lines.save(load(companyId, lineId).withApproved());
        audit(companyId, saved, "Discount approved");
        return factory.toResponse(saved, access.can(RepairPermissions.PRICE_VIEW));
    }

    @Override
    @Transactional
    public LineResponse setDisposition(CompanyId companyId, UUID lineId, DispositionCommand command) {
        access.require(RepairPermissions.LINE_EDIT);
        RepairLine l = load(companyId, lineId);
        if (l.type() != LineType.REMOVED_PART) {
            throw new RepairDomainException("error.repair.disposition.type", null, "Only a removed part has a disposition");
        }
        RepairLine saved = lines.save(l.withDisposition(command.disposition()));
        audit(companyId, saved, "Disposition set to " + command.disposition());
        return factory.toResponse(saved, access.can(RepairPermissions.PRICE_VIEW));
    }

    private RepairLine load(CompanyId companyId, UUID id) {
        return lines.find(companyId, id).orElseThrow(() -> RepairSupport.notFound("Line"));
    }

    private static void requireEditable(RepairLine l) {
        if (!l.isEditable()) {
            throw new RepairDomainException("error.repair.line.locked", null,
                    "This line is already in a quotation; change it through a new quotation version");
        }
    }

    private void audit(CompanyId companyId, RepairLine l, String message) {
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, l.id(), message,
                Map.of("order", l.orderId().toString(), "type", l.type().name(), "by", access.actorLabel()));
    }
}
