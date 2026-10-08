package com.bradox.erp.repair.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.LaborCategory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LaborRepository {

    List<LaborCategory> categories(CompanyId companyId, boolean includeInactive);

    Optional<LaborCategory> findCategory(CompanyId companyId, UUID id);

    LaborCategory save(LaborCategory category);

    /** Text search over code and the three descriptions; {@code null} query lists everything. */
    List<GuideEntry> guide(CompanyId companyId, String query, boolean includeInactive);

    Optional<GuideEntry> findGuide(CompanyId companyId, UUID id);

    /** The entry with this code for exactly this make and model (both may be null). */
    Optional<GuideEntry> findGuideByKey(CompanyId companyId, String code, String make, String model);

    List<GuideEntry> findGuides(CompanyId companyId, Collection<UUID> ids);

    GuideEntry save(GuideEntry entry);

    void saveAllGuide(Collection<GuideEntry> entries);
}
