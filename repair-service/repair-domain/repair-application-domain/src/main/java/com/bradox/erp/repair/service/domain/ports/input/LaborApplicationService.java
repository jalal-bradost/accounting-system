package com.bradox.erp.repair.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.LaborCategory;
import com.bradox.erp.repair.service.domain.dto.GuideEntryCommand;
import com.bradox.erp.repair.service.domain.dto.GuideImportCommand;
import com.bradox.erp.repair.service.domain.dto.GuideImportResponse;
import com.bradox.erp.repair.service.domain.dto.LaborCategoryCommand;

import java.util.List;
import java.util.UUID;

public interface LaborApplicationService {

    List<LaborCategory> categories(CompanyId companyId, boolean includeInactive);

    LaborCategory saveCategory(CompanyId companyId, UUID id, LaborCategoryCommand command);

    /** Entries matching the text; with a make, those for that vehicle (and generic ones) come first. */
    List<GuideEntry> guide(CompanyId companyId, String query, String make, String model, Integer year, boolean includeInactive);

    GuideEntry saveGuide(CompanyId companyId, UUID id, GuideEntryCommand command);

    GuideImportResponse importGuide(CompanyId companyId, GuideImportCommand command);
}
