package com.bradox.erp.repair.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.Finding;
import com.bradox.erp.repair.domain.core.model.InspectionTemplate;
import com.bradox.erp.repair.service.domain.dto.FindingCommand;
import com.bradox.erp.repair.service.domain.dto.InspectionResponse;
import com.bradox.erp.repair.service.domain.dto.LineFromFindingCommand;
import com.bradox.erp.repair.service.domain.dto.LineResponse;
import com.bradox.erp.repair.service.domain.dto.SaveInspectionCommand;
import com.bradox.erp.repair.service.domain.dto.TemplateCommand;

import java.util.List;
import java.util.UUID;

public interface InspectionApplicationService {

    List<InspectionTemplate> templates(CompanyId companyId, boolean includeInactive);

    InspectionTemplate saveTemplate(CompanyId companyId, UUID id, TemplateCommand command);

    /** The order's inspection; created from the default template on first use (REP-02 #8). */
    InspectionResponse start(CompanyId companyId, UUID orderId, UUID templateId);

    InspectionResponse get(CompanyId companyId, UUID orderId);

    InspectionResponse save(CompanyId companyId, UUID orderId, SaveInspectionCommand command);

    InspectionResponse signOff(CompanyId companyId, UUID orderId);

    List<Finding> findings(CompanyId companyId, UUID orderId);

    Finding addFinding(CompanyId companyId, UUID orderId, FindingCommand command);

    /** "Add to quote": a draft line (or the lines of a package) linked to the finding (REP-02 #3). */
    List<LineResponse> addToQuote(CompanyId companyId, UUID orderId, UUID findingId, LineFromFindingCommand command);
}
