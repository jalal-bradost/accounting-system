package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.model.Finding;
import com.bradox.erp.repair.domain.core.model.Inspection;
import com.bradox.erp.repair.domain.core.model.InspectionTemplate;
import com.bradox.erp.repair.domain.core.model.RepairLine;
import com.bradox.erp.repair.domain.core.model.ServicePackage;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.domain.core.valueobject.InspectionResultCode;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.service.domain.dto.AddLineCommand;
import com.bradox.erp.repair.service.domain.dto.FindingCommand;
import com.bradox.erp.repair.service.domain.dto.InspectionResponse;
import com.bradox.erp.repair.service.domain.dto.InspectionSummary;
import com.bradox.erp.repair.service.domain.dto.LineFromFindingCommand;
import com.bradox.erp.repair.service.domain.dto.LineResponse;
import com.bradox.erp.repair.service.domain.dto.SaveInspectionCommand;
import com.bradox.erp.repair.service.domain.dto.TemplateCommand;
import com.bradox.erp.repair.service.domain.ports.input.InspectionApplicationService;
import com.bradox.erp.repair.service.domain.ports.output.repository.InspectionRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.PackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class InspectionApplicationServiceImpl implements InspectionApplicationService {

    private static final String AUDIT_MODEL = "rep.inspection";

    private final InspectionRepository inspections;
    private final PackageRepository packages;
    private final LineFactory factory;
    private final RepairSupport support;
    private final RepairAccess access;
    private final AuditLogPort audit;

    InspectionApplicationServiceImpl(InspectionRepository inspections, PackageRepository packages, LineFactory factory,
                                     RepairSupport support, RepairAccess access, AuditLogPort audit) {
        this.inspections = inspections;
        this.packages = packages;
        this.factory = factory;
        this.support = support;
        this.access = access;
        this.audit = audit;
    }

    @Override
    @Transactional
    public List<InspectionTemplate> templates(CompanyId companyId, boolean includeInactive) {
        access.require(RepairPermissions.ORDER_VIEW);
        support.settings(companyId);
        return inspections.templates(companyId, includeInactive);
    }

    @Override
    @Transactional
    public InspectionTemplate saveTemplate(CompanyId companyId, UUID id, TemplateCommand c) {
        access.require(RepairPermissions.SETTINGS_MANAGE);
        if (id != null) {
            inspections.findTemplate(companyId, id).orElseThrow(() -> RepairSupport.notFound("Template"));
        }
        List<InspectionTemplate.Item> items = new ArrayList<>();
        int seq = 0;
        for (TemplateCommand.Item i : c.items() == null ? List.<TemplateCommand.Item>of() : c.items()) {
            if (i.label() == null || i.label().isBlank()) {
                throw new RepairDomainException("error.repair.template.label", null, "Every checklist item needs a label");
            }
            items.add(new InspectionTemplate.Item(UUID.randomUUID(), i.section() == null || i.section().isBlank() ? "General" : i.section().trim(),
                    i.label().trim(), seq++));
        }
        if (items.isEmpty()) {
            throw new RepairDomainException("error.repair.template.empty", null, "A template needs at least one item");
        }
        InspectionTemplate saved = inspections.save(new InspectionTemplate(id != null ? id : UUID.randomUUID(), companyId.getId(),
                c.name().trim(), c.vehicleType(), c.active(), items));
        audit.recordBusinessEvent(companyId, "rep.inspection_template", saved.id(), "Inspection template saved", Map.of("name", saved.name()));
        return saved;
    }

    @Override
    @Transactional
    public InspectionResponse start(CompanyId companyId, UUID orderId, UUID templateId) {
        access.require(RepairPermissions.DIAGNOSIS_EDIT);
        Inspection existing = inspections.findByOrder(companyId, orderId).orElse(null);
        if (existing != null) {
            return respond(existing);
        }
        Settings settings = support.settings(companyId);
        UUID chosen = templateId != null ? templateId : settings.defaultInspectionTemplateId();
        InspectionTemplate template = chosen == null ? null : inspections.findTemplate(companyId, chosen).orElse(null);
        List<Inspection.Result> results = new ArrayList<>();
        if (template != null) {
            int seq = 0;
            for (InspectionTemplate.Item i : template.items()) {
                results.add(new Inspection.Result(UUID.randomUUID(), seq++, i.section(), i.label(), InspectionResultCode.NOT_CHECKED,
                        null, null, null));
            }
        }
        Inspection saved = inspections.save(new Inspection(UUID.randomUUID(), companyId.getId(), orderId,
                template == null ? null : template.id(), access.actorLabel(), access.now(), null, null, null, null, results));
        audit(companyId, saved.id(), "Inspection started", orderId);
        return respond(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionResponse get(CompanyId companyId, UUID orderId) {
        access.require(RepairPermissions.ORDER_VIEW);
        return respond(inspections.findByOrder(companyId, orderId).orElseThrow(() -> RepairSupport.notFound("Inspection")));
    }

    @Override
    @Transactional
    public InspectionResponse save(CompanyId companyId, UUID orderId, SaveInspectionCommand c) {
        access.require(RepairPermissions.DIAGNOSIS_EDIT);
        Inspection i = inspections.findByOrder(companyId, orderId).orElseThrow(() -> RepairSupport.notFound("Inspection"));
        if (i.isSignedOff()) {
            throw new RepairDomainException("error.repair.inspection.signedOff", null,
                    "The inspection is signed off; record new discoveries as findings");
        }
        if (c.odometerKm() != null && c.odometerKm() < 0) {
            throw new RepairDomainException("error.repair.inspection.odometer", null, "The odometer cannot be negative");
        }
        List<Inspection.Result> results = new ArrayList<>();
        int seq = 0;
        List<SaveInspectionCommand.Result> incoming = c.results() == null ? List.of() : c.results();
        for (SaveInspectionCommand.Result r : incoming) {
            if (r.itemLabel() == null || r.itemLabel().isBlank() || r.result() == null) {
                throw new RepairDomainException("error.repair.inspection.item", null, "Each result needs an item and a result");
            }
            Inspection.Result old = i.results().stream()
                    .filter(o -> o.itemLabel().equals(r.itemLabel()) && java.util.Objects.equals(o.section(), r.section())).findFirst().orElse(null);
            results.add(new Inspection.Result(old != null ? old.id() : UUID.randomUUID(), seq++,
                    r.section() == null ? "General" : r.section(), r.itemLabel(), r.result(), r.note(), r.photoDocumentId(),
                    old == null ? null : old.recommendedLineId()));
        }
        Inspection saved = inspections.save(new Inspection(i.id(), i.companyId(), i.orderId(), i.templateId(), i.performedBy(),
                i.startedAt(), null, null, c.odometerKm() != null ? c.odometerKm() : i.odometerKm(),
                c.notes() != null ? c.notes() : i.notes(), incoming.isEmpty() ? i.results() : results));
        audit(companyId, saved.id(), "Inspection saved", orderId);
        return respond(saved);
    }

    @Override
    @Transactional
    public InspectionResponse signOff(CompanyId companyId, UUID orderId) {
        access.require(RepairPermissions.DIAGNOSIS_EDIT);
        Inspection i = inspections.findByOrder(companyId, orderId).orElseThrow(() -> RepairSupport.notFound("Inspection"));
        if (i.isSignedOff()) {
            return respond(i);
        }
        Inspection saved = inspections.save(new Inspection(i.id(), i.companyId(), i.orderId(), i.templateId(), i.performedBy(),
                i.startedAt(), access.now(), access.actorLabel(), i.odometerKm(), i.notes(), i.results()));
        audit(companyId, saved.id(), "Inspection signed off", orderId);
        return respond(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Finding> findings(CompanyId companyId, UUID orderId) {
        access.require(RepairPermissions.ORDER_VIEW);
        return inspections.findings(companyId, orderId);
    }

    @Override
    @Transactional
    public Finding addFinding(CompanyId companyId, UUID orderId, FindingCommand c) {
        access.require(RepairPermissions.DIAGNOSIS_EDIT);
        Finding saved = inspections.save(new Finding(UUID.randomUUID(), companyId.getId(), orderId, c.severity(),
                c.description().trim(), c.cause(), c.recommendedAction(), c.customerVisible(), access.now(), access.actorLabel()));
        audit.recordBusinessEvent(companyId, "rep.finding", saved.id(), "Finding added",
                Map.of("order", orderId.toString(), "severity", c.severity().name(), "by", access.actorLabel()));
        return saved;
    }

    @Override
    @Transactional
    public List<LineResponse> addToQuote(CompanyId companyId, UUID orderId, UUID findingId, LineFromFindingCommand c) {
        access.require(RepairPermissions.LINE_EDIT);
        Finding f = inspections.findFinding(companyId, findingId).orElseThrow(() -> RepairSupport.notFound("Finding"));
        if (!f.orderId().equals(orderId)) {
            throw RepairSupport.notFound("Finding");
        }
        Settings settings = support.settings(companyId);
        boolean prices = access.can(RepairPermissions.PRICE_VIEW);
        List<RepairLine> created;
        if (c.packageId() != null) {
            ServicePackage pkg = packages.find(companyId, c.packageId()).orElseThrow(() -> RepairSupport.notFound("Package"));
            if (!pkg.active()) {
                throw new RepairDomainException("error.repair.package.archived", null, "This package is archived and cannot be added");
            }
            created = factory.fromPackage(companyId, orderId, pkg, settings, findingId);
        } else {
            String description = c.description() != null && !c.description().isBlank() ? c.description()
                    : f.recommendedAction() != null && !f.recommendedAction().isBlank() ? f.recommendedAction() : f.description();
            created = List.of(factory.create(companyId, orderId, new AddLineCommand(c.type() == null ? LineType.OPERATION : c.type(),
                    null, c.productId(), description, null, null, null, c.laborGuideId(), null, null, null, null, null),
                    settings, findingId, null, null));
        }
        return created.stream().map(l -> factory.toResponse(l, prices)).toList();
    }

    private InspectionResponse respond(Inspection i) {
        return new InspectionResponse(i, new InspectionSummary(i.count(InspectionResultCode.OK), i.count(InspectionResultCode.ATTENTION),
                i.count(InspectionResultCode.URGENT), i.count(InspectionResultCode.NOT_CHECKED)));
    }

    private void audit(CompanyId companyId, UUID id, String message, UUID orderId) {
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, message, Map.of("order", orderId.toString(), "by", access.actorLabel()));
    }
}
