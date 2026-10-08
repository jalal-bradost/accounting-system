package com.bradox.erp.repair.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.dataaccess.entity.LaborCategoryEntity;
import com.bradox.erp.repair.dataaccess.entity.LaborGuideEntity;
import com.bradox.erp.repair.dataaccess.repository.LaborCategoryJpaRepository;
import com.bradox.erp.repair.dataaccess.repository.LaborGuideJpaRepository;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.LaborCategory;
import com.bradox.erp.repair.service.domain.ports.output.repository.LaborRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class LaborRepositoryImpl implements LaborRepository {

    private final LaborCategoryJpaRepository categories;
    private final LaborGuideJpaRepository guide;

    public LaborRepositoryImpl(LaborCategoryJpaRepository categories, LaborGuideJpaRepository guide) {
        this.categories = categories;
        this.guide = guide;
    }

    @Override
    public List<LaborCategory> categories(CompanyId companyId, boolean includeInactive) {
        return categories.findByCompanyIdOrderByNameAsc(companyId.getId()).stream()
                .filter(e -> includeInactive || e.active).map(LaborRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<LaborCategory> findCategory(CompanyId companyId, UUID id) {
        return categories.findById(id).filter(e -> e.companyId.equals(companyId.getId())).map(LaborRepositoryImpl::toDomain);
    }

    @Override
    public LaborCategory save(LaborCategory c) {
        LaborCategoryEntity e = categories.findById(c.id()).orElseGet(LaborCategoryEntity::new);
        e.id = c.id();
        e.companyId = c.companyId();
        e.name = c.name();
        e.hourlyRate = c.hourlyRate();
        e.active = c.active();
        return toDomain(categories.saveAndFlush(e));
    }

    @Override
    public List<GuideEntry> guide(CompanyId companyId, String query, boolean includeInactive) {
        String q = query == null ? null : query.toLowerCase(Locale.ROOT);
        return guide.findByCompanyId(companyId.getId()).stream()
                .filter(e -> includeInactive || e.active)
                .filter(e -> q == null || contains(e.code, q) || contains(e.descriptionEn, q) || contains(e.descriptionAr, q)
                        || contains(e.descriptionKu, q))
                .map(LaborRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<GuideEntry> findGuide(CompanyId companyId, UUID id) {
        return guide.findById(id).filter(e -> e.companyId.equals(companyId.getId())).map(LaborRepositoryImpl::toDomain);
    }

    @Override
    public Optional<GuideEntry> findGuideByKey(CompanyId companyId, String code, String make, String model) {
        return guide.findByCompanyIdAndCodeIgnoreCase(companyId.getId(), code).stream()
                .filter(e -> same(e.make, make) && same(e.model, model)).findFirst().map(LaborRepositoryImpl::toDomain);
    }

    @Override
    public List<GuideEntry> findGuides(CompanyId companyId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of()
                : guide.findByCompanyIdAndIdIn(companyId.getId(), ids).stream().map(LaborRepositoryImpl::toDomain).toList();
    }

    @Override
    public GuideEntry save(GuideEntry g) {
        return toDomain(guide.saveAndFlush(apply(g)));
    }

    @Override
    public void saveAllGuide(Collection<GuideEntry> entries) {
        guide.saveAllAndFlush(entries.stream().map(this::apply).toList());
    }

    private LaborGuideEntity apply(GuideEntry g) {
        LaborGuideEntity e = guide.findById(g.id()).orElseGet(LaborGuideEntity::new);
        e.id = g.id();
        e.companyId = g.companyId();
        e.code = g.code();
        e.descriptionEn = g.descriptionEn();
        e.descriptionAr = g.descriptionAr();
        e.descriptionKu = g.descriptionKu();
        e.laborCategoryId = g.laborCategoryId();
        e.standardMinutes = g.standardMinutes();
        e.make = g.make();
        e.model = g.model();
        e.yearFrom = g.yearFrom();
        e.yearTo = g.yearTo();
        e.active = g.active();
        return e;
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private static boolean same(String a, String b) {
        return Objects.equals(a == null ? null : a.toLowerCase(Locale.ROOT), b == null ? null : b.toLowerCase(Locale.ROOT));
    }

    private static LaborCategory toDomain(LaborCategoryEntity e) {
        return new LaborCategory(e.id, e.companyId, e.name, e.hourlyRate, e.active);
    }

    private static GuideEntry toDomain(LaborGuideEntity e) {
        return new GuideEntry(e.id, e.companyId, e.code, e.descriptionEn, e.descriptionAr, e.descriptionKu, e.laborCategoryId,
                e.standardMinutes, e.make, e.model, e.yearFrom, e.yearTo, e.active);
    }
}
