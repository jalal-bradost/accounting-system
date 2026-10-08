package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.repair.domain.core.exception.RepairDomainException;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.LaborCategory;
import com.bradox.erp.repair.domain.core.rule.GuideRanking;
import com.bradox.erp.repair.domain.core.rule.LaborGuideCsv;
import com.bradox.erp.repair.service.domain.dto.GuideEntryCommand;
import com.bradox.erp.repair.service.domain.dto.GuideImportCommand;
import com.bradox.erp.repair.service.domain.dto.GuideImportResponse;
import com.bradox.erp.repair.service.domain.dto.LaborCategoryCommand;
import com.bradox.erp.repair.service.domain.ports.input.LaborApplicationService;
import com.bradox.erp.repair.service.domain.ports.output.repository.LaborRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Validated
class LaborApplicationServiceImpl implements LaborApplicationService {

    private final RepairSupport support;
    private final LaborRepository labor;
    private final RepairAccess access;
    private final AuditLogPort audit;

    LaborApplicationServiceImpl(RepairSupport support, LaborRepository labor, RepairAccess access, AuditLogPort audit) {
        this.support = support;
        this.labor = labor;
        this.access = access;
        this.audit = audit;
    }

    @Override
    @Transactional
    public List<LaborCategory> categories(CompanyId companyId, boolean includeInactive) {
        access.require(RepairPermissions.ORDER_VIEW);
        support.settings(companyId);
        return labor.categories(companyId, includeInactive);
    }

    @Override
    @Transactional
    public LaborCategory saveCategory(CompanyId companyId, UUID id, LaborCategoryCommand c) {
        access.require(RepairPermissions.LABOR_GUIDE_MANAGE);
        if (c.hourlyRate() != null && c.hourlyRate().signum() < 0) {
            throw new RepairDomainException("error.repair.rate", null, "The hourly rate cannot be negative");
        }
        if (id != null) {
            labor.findCategory(companyId, id).orElseThrow(() -> RepairSupport.notFound("Labor category"));
        }
        LaborCategory saved = labor.save(new LaborCategory(id != null ? id : UUID.randomUUID(), companyId.getId(), c.name().trim(),
                c.hourlyRate(), c.active()));
        audit.recordBusinessEvent(companyId, "rep.labor_category", saved.id(), "Labor category saved", Map.of("name", saved.name()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuideEntry> guide(CompanyId companyId, String query, String make, String model, Integer year, boolean includeInactive) {
        access.require(RepairPermissions.ORDER_VIEW);
        String q = query == null || query.isBlank() ? null : query.trim();
        return GuideRanking.rank(labor.guide(companyId, q, includeInactive), make, model, year);
    }

    @Override
    @Transactional
    public GuideEntry saveGuide(CompanyId companyId, UUID id, GuideEntryCommand c) {
        access.require(RepairPermissions.LABOR_GUIDE_MANAGE);
        if (blank(c.descriptionEn()) && blank(c.descriptionAr()) && blank(c.descriptionKu())) {
            throw new RepairDomainException("error.repair.guide.description", null, "A description in at least one language is required");
        }
        if (!blank(c.model()) && blank(c.make())) {
            throw new RepairDomainException("error.repair.guide.model", null, "A model needs a make");
        }
        if (c.yearFrom() != null && c.yearTo() != null && c.yearFrom() > c.yearTo()) {
            throw new RepairDomainException("error.repair.guide.years", null, "The first year is after the last year");
        }
        if (id != null) {
            labor.findGuide(companyId, id).orElseThrow(() -> RepairSupport.notFound("Guide entry"));
        }
        String make = trim(c.make());
        String model = trim(c.model());
        labor.findGuideByKey(companyId, c.code().trim(), make, model).ifPresent(existing -> {
            if (!existing.id().equals(id)) {
                throw new RepairDomainException("error.repair.guide.duplicate", null, "This code already exists for the same make and model");
            }
        });
        GuideEntry saved = labor.save(new GuideEntry(id != null ? id : UUID.randomUUID(), companyId.getId(), c.code().trim(),
                trim(c.descriptionEn()), trim(c.descriptionAr()), trim(c.descriptionKu()), c.laborCategoryId(), c.standardMinutes(),
                make, model, c.yearFrom(), c.yearTo(), c.active()));
        audit.recordBusinessEvent(companyId, "rep.labor_guide", saved.id(), "Labor guide entry saved", Map.of("code", saved.code()));
        return saved;
    }

    @Override
    @Transactional
    public GuideImportResponse importGuide(CompanyId companyId, GuideImportCommand command) {
        access.require(RepairPermissions.LABOR_GUIDE_MANAGE);
        LaborGuideCsv.Result parsed = LaborGuideCsv.parse(command.csv());
        if (command.allOrNothing() && !parsed.errors().isEmpty()) {
            return new GuideImportResponse(0, 0, parsed.errors().size(), false, parsed.errors());
        }
        Map<String, UUID> categories = new java.util.HashMap<>();
        labor.categories(companyId, true).forEach(c -> categories.put(c.name().toLowerCase(Locale.ROOT), c.id()));
        List<GuideEntry> toSave = new ArrayList<>();
        int created = 0;
        int updated = 0;
        for (LaborGuideCsv.Row r : parsed.rows()) {
            UUID categoryId = null;
            if (r.category() != null) {
                categoryId = categories.get(r.category().toLowerCase(Locale.ROOT));
                if (categoryId == null) {
                    categoryId = labor.save(new LaborCategory(UUID.randomUUID(), companyId.getId(), r.category(), null, true)).id();
                    categories.put(r.category().toLowerCase(Locale.ROOT), categoryId);
                }
            }
            GuideEntry existing = labor.findGuideByKey(companyId, r.code(), r.make(), r.model()).orElse(null);
            toSave.add(new GuideEntry(existing != null ? existing.id() : UUID.randomUUID(), companyId.getId(), r.code(),
                    r.descriptionEn(), r.descriptionAr(), r.descriptionKu(), categoryId, r.standardMinutes(), r.make(), r.model(),
                    r.yearFrom(), r.yearTo(), true));
            if (existing != null) {
                updated++;
            } else {
                created++;
            }
        }
        labor.saveAllGuide(toSave);
        audit.recordBusinessEvent(companyId, "rep.labor_guide", companyId.getId(), "Labor guide imported",
                Map.of("created", created, "updated", updated, "rejected", parsed.errors().size()));
        return new GuideImportResponse(created, updated, parsed.errors().size(), true, parsed.errors());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return blank(s) ? null : s.trim();
    }
}
