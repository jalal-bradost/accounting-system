package com.bradox.erp.repair.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.dataaccess.entity.PackageEntity;
import com.bradox.erp.repair.dataaccess.entity.PackageLineEntity;
import com.bradox.erp.repair.dataaccess.repository.PackageJpaRepository;
import com.bradox.erp.repair.dataaccess.repository.PackageLineJpaRepository;
import com.bradox.erp.repair.domain.core.model.PackageLine;
import com.bradox.erp.repair.domain.core.model.ServicePackage;
import com.bradox.erp.repair.domain.core.valueobject.LineType;
import com.bradox.erp.repair.service.domain.ports.output.repository.PackageRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PackageRepositoryImpl implements PackageRepository {

    private final PackageJpaRepository packages;
    private final PackageLineJpaRepository lines;

    public PackageRepositoryImpl(PackageJpaRepository packages, PackageLineJpaRepository lines) {
        this.packages = packages;
        this.lines = lines;
    }

    @Override
    public List<ServicePackage> list(CompanyId companyId, String query, boolean includeArchived) {
        String q = query == null ? null : query.toLowerCase(Locale.ROOT);
        List<PackageEntity> found = packages.findByCompanyIdOrderByNameAsc(companyId.getId()).stream()
                .filter(e -> includeArchived || e.active)
                .filter(e -> q == null || e.name.toLowerCase(Locale.ROOT).contains(q)
                        || (e.code != null && e.code.toLowerCase(Locale.ROOT).contains(q))).toList();
        Map<UUID, List<PackageLine>> byPackage = linesOf(found.stream().map(e -> e.id).toList());
        return found.stream().map(e -> toDomain(e, byPackage.getOrDefault(e.id, List.of()))).toList();
    }

    @Override
    public Optional<ServicePackage> find(CompanyId companyId, UUID id) {
        return packages.findById(id).filter(e -> e.companyId.equals(companyId.getId()))
                .map(e -> toDomain(e, linesOf(List.of(e.id)).getOrDefault(e.id, List.of())));
    }

    @Override
    public ServicePackage save(ServicePackage p) {
        PackageEntity e = packages.findById(p.id()).orElseGet(PackageEntity::new);
        e.id = p.id();
        e.companyId = p.companyId();
        e.code = p.code();
        e.name = p.name();
        e.category = p.category();
        e.active = p.active();
        e.createdAt = p.createdAt();
        e.createdBy = p.createdBy();
        packages.saveAndFlush(e);
        lines.deleteByPackageId(p.id());
        lines.flush();
        lines.saveAllAndFlush(p.lines().stream().map(l -> {
            PackageLineEntity le = new PackageLineEntity();
            le.id = l.id();
            le.packageId = p.id();
            le.sequence = l.sequence();
            le.lineType = l.type().name();
            le.productId = l.productId();
            le.laborGuideId = l.laborGuideId();
            le.description = l.description();
            le.qty = l.qty();
            le.standardMinutes = l.standardMinutes();
            return le;
        }).toList());
        return find(new CompanyId(p.companyId()), p.id()).orElseThrow();
    }

    private Map<UUID, List<PackageLine>> linesOf(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return lines.findByPackageIdInOrderBySequenceAsc(ids).stream().collect(Collectors.groupingBy(l -> l.packageId,
                Collectors.mapping(l -> new PackageLine(l.id, l.sequence, LineType.valueOf(l.lineType), l.productId, l.laborGuideId,
                        l.description, l.qty, l.standardMinutes), Collectors.toList())));
    }

    private static ServicePackage toDomain(PackageEntity e, List<PackageLine> lines) {
        return new ServicePackage(e.id, e.companyId, e.code, e.name, e.category, e.active, e.createdAt, e.createdBy, lines);
    }
}
