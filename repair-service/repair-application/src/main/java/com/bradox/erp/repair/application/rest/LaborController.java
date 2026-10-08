package com.bradox.erp.repair.application.rest;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.web.CurrentCompany;
import com.bradox.erp.repair.domain.core.model.GuideEntry;
import com.bradox.erp.repair.domain.core.model.LaborCategory;
import com.bradox.erp.repair.service.domain.dto.GuideEntryCommand;
import com.bradox.erp.repair.service.domain.dto.GuideImportCommand;
import com.bradox.erp.repair.service.domain.dto.GuideImportResponse;
import com.bradox.erp.repair.service.domain.dto.LaborCategoryCommand;
import com.bradox.erp.repair.service.domain.ports.input.LaborApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/repair", produces = "application/json")
public class LaborController {

    private final LaborApplicationService labor;

    public LaborController(LaborApplicationService labor) {
        this.labor = labor;
    }

    @GetMapping("/labor-categories")
    public List<LaborCategory> categories(@CurrentCompany CompanyId companyId,
                                          @RequestParam(defaultValue = "false") boolean includeInactive) {
        return labor.categories(companyId, includeInactive);
    }

    @PostMapping("/labor-categories")
    public LaborCategory createCategory(@CurrentCompany CompanyId companyId, @Valid @RequestBody LaborCategoryCommand command) {
        return labor.saveCategory(companyId, null, command);
    }

    @PutMapping("/labor-categories/{id}")
    public LaborCategory updateCategory(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                        @Valid @RequestBody LaborCategoryCommand command) {
        return labor.saveCategory(companyId, id, command);
    }

    @GetMapping("/labor-guide")
    public List<GuideEntry> guide(@CurrentCompany CompanyId companyId, @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String make, @RequestParam(required = false) String model,
                                  @RequestParam(required = false) Integer year,
                                  @RequestParam(defaultValue = "false") boolean includeInactive) {
        return labor.guide(companyId, q, make, model, year, includeInactive);
    }

    @PostMapping("/labor-guide")
    public GuideEntry createGuide(@CurrentCompany CompanyId companyId, @Valid @RequestBody GuideEntryCommand command) {
        return labor.saveGuide(companyId, null, command);
    }

    @PutMapping("/labor-guide/{id}")
    public GuideEntry updateGuide(@CurrentCompany CompanyId companyId, @PathVariable UUID id,
                                  @Valid @RequestBody GuideEntryCommand command) {
        return labor.saveGuide(companyId, id, command);
    }

    @PostMapping("/labor-guide/import")
    public GuideImportResponse importGuide(@CurrentCompany CompanyId companyId, @Valid @RequestBody GuideImportCommand command) {
        return labor.importGuide(companyId, command);
    }
}
