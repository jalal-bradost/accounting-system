package com.bradox.erp.repair.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.dataaccess.entity.FindingEntity;
import com.bradox.erp.repair.dataaccess.entity.InspectionEntity;
import com.bradox.erp.repair.dataaccess.entity.InspectionResultEntity;
import com.bradox.erp.repair.dataaccess.entity.InspectionTemplateEntity;
import com.bradox.erp.repair.dataaccess.entity.InspectionTemplateItemEntity;
import com.bradox.erp.repair.dataaccess.repository.FindingJpaRepository;
import com.bradox.erp.repair.dataaccess.repository.InspectionJpaRepository;
import com.bradox.erp.repair.dataaccess.repository.InspectionResultJpaRepository;
import com.bradox.erp.repair.dataaccess.repository.InspectionTemplateItemJpaRepository;
import com.bradox.erp.repair.dataaccess.repository.InspectionTemplateJpaRepository;
import com.bradox.erp.repair.domain.core.model.Finding;
import com.bradox.erp.repair.domain.core.model.Inspection;
import com.bradox.erp.repair.domain.core.model.InspectionTemplate;
import com.bradox.erp.repair.domain.core.valueobject.InspectionResultCode;
import com.bradox.erp.repair.domain.core.valueobject.Severity;
import com.bradox.erp.repair.service.domain.ports.output.repository.InspectionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class InspectionRepositoryImpl implements InspectionRepository {

    private final InspectionTemplateJpaRepository templates;
    private final InspectionTemplateItemJpaRepository items;
    private final InspectionJpaRepository inspections;
    private final InspectionResultJpaRepository results;
    private final FindingJpaRepository findings;

    public InspectionRepositoryImpl(InspectionTemplateJpaRepository templates, InspectionTemplateItemJpaRepository items,
                                    InspectionJpaRepository inspections, InspectionResultJpaRepository results,
                                    FindingJpaRepository findings) {
        this.templates = templates;
        this.items = items;
        this.inspections = inspections;
        this.results = results;
        this.findings = findings;
    }

    @Override
    public List<InspectionTemplate> templates(CompanyId companyId, boolean includeInactive) {
        List<InspectionTemplateEntity> found = templates.findByCompanyIdOrderByNameAsc(companyId.getId()).stream()
                .filter(e -> includeInactive || e.active).toList();
        Map<UUID, List<InspectionTemplate.Item>> byTemplate = itemsOf(found.stream().map(e -> e.id).toList());
        return found.stream().map(e -> toDomain(e, byTemplate.getOrDefault(e.id, List.of()))).toList();
    }

    @Override
    public Optional<InspectionTemplate> findTemplate(CompanyId companyId, UUID id) {
        return templates.findById(id).filter(e -> e.companyId.equals(companyId.getId()))
                .map(e -> toDomain(e, itemsOf(List.of(e.id)).getOrDefault(e.id, List.of())));
    }

    @Override
    public InspectionTemplate save(InspectionTemplate t) {
        InspectionTemplateEntity e = templates.findById(t.id()).orElseGet(InspectionTemplateEntity::new);
        e.id = t.id();
        e.companyId = t.companyId();
        e.name = t.name();
        e.vehicleType = t.vehicleType();
        e.active = t.active();
        templates.saveAndFlush(e);
        items.deleteByTemplateId(t.id());
        items.flush();
        items.saveAllAndFlush(t.items().stream().map(i -> {
            InspectionTemplateItemEntity ie = new InspectionTemplateItemEntity();
            ie.id = i.id();
            ie.templateId = t.id();
            ie.section = i.section();
            ie.label = i.label();
            ie.sequence = i.sequence();
            return ie;
        }).toList());
        return findTemplate(new CompanyId(t.companyId()), t.id()).orElseThrow();
    }

    @Override
    public Optional<Inspection> findByOrder(CompanyId companyId, UUID orderId) {
        return inspections.findByCompanyIdAndOrderId(companyId.getId(), orderId).map(this::toDomain);
    }

    @Override
    public Inspection save(Inspection i) {
        InspectionEntity e = inspections.findById(i.id()).orElseGet(InspectionEntity::new);
        e.id = i.id();
        e.companyId = i.companyId();
        e.orderId = i.orderId();
        e.templateId = i.templateId();
        e.performedBy = i.performedBy();
        e.startedAt = i.startedAt();
        e.signedOffAt = i.signedOffAt();
        e.signedOffBy = i.signedOffBy();
        e.odometerKm = i.odometerKm();
        e.notes = i.notes();
        inspections.saveAndFlush(e);
        results.deleteByInspectionId(i.id());
        results.flush();
        results.saveAllAndFlush(i.results().stream().map(r -> {
            InspectionResultEntity re = new InspectionResultEntity();
            re.id = r.id();
            re.inspectionId = i.id();
            re.sequence = r.sequence();
            re.section = r.section();
            re.itemLabel = r.itemLabel();
            re.result = r.result().name();
            re.note = r.note();
            re.photoDocumentId = r.photoDocumentId();
            re.recommendedLineId = r.recommendedLineId();
            return re;
        }).toList());
        return toDomain(inspections.findById(i.id()).orElseThrow());
    }

    @Override
    public List<Finding> findings(CompanyId companyId, UUID orderId) {
        return findings.findByCompanyIdAndOrderIdOrderByCreatedAtAsc(companyId.getId(), orderId).stream()
                .map(InspectionRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<Finding> findFinding(CompanyId companyId, UUID id) {
        return findings.findByCompanyIdAndId(companyId.getId(), id).map(InspectionRepositoryImpl::toDomain);
    }

    @Override
    public Finding save(Finding f) {
        FindingEntity e = findings.findById(f.id()).orElseGet(FindingEntity::new);
        e.id = f.id();
        e.companyId = f.companyId();
        e.orderId = f.orderId();
        e.severity = f.severity().name();
        e.description = f.description();
        e.cause = f.cause();
        e.recommendedAction = f.recommendedAction();
        e.customerVisible = f.customerVisible();
        e.createdAt = f.createdAt();
        e.createdBy = f.createdBy();
        return toDomain(findings.saveAndFlush(e));
    }

    private Map<UUID, List<InspectionTemplate.Item>> itemsOf(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return items.findByTemplateIdInOrderBySequenceAsc(ids).stream().collect(Collectors.groupingBy(i -> i.templateId,
                Collectors.mapping(i -> new InspectionTemplate.Item(i.id, i.section, i.label, i.sequence), Collectors.toList())));
    }

    private Inspection toDomain(InspectionEntity e) {
        List<Inspection.Result> rs = results.findByInspectionIdOrderBySequenceAsc(e.id).stream()
                .map(r -> new Inspection.Result(r.id, r.sequence, r.section, r.itemLabel, InspectionResultCode.valueOf(r.result),
                        r.note, r.photoDocumentId, r.recommendedLineId)).toList();
        return new Inspection(e.id, e.companyId, e.orderId, e.templateId, e.performedBy, e.startedAt, e.signedOffAt, e.signedOffBy,
                e.odometerKm, e.notes, rs);
    }

    private static InspectionTemplate toDomain(InspectionTemplateEntity e, List<InspectionTemplate.Item> items) {
        return new InspectionTemplate(e.id, e.companyId, e.name, e.vehicleType, e.active, items);
    }

    private static Finding toDomain(FindingEntity e) {
        return new Finding(e.id, e.companyId, e.orderId, Severity.valueOf(e.severity), e.description, e.cause, e.recommendedAction,
                e.customerVisible, e.createdAt, e.createdBy);
    }
}
