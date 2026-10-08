package com.bradox.erp.sign.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.service.domain.dto.CreateTemplateCommand;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.ReplaceDocumentCommand;
import com.bradox.erp.sign.service.domain.dto.TemplateResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateSummaryResponse;
import com.bradox.erp.sign.service.domain.dto.UpdateTemplateCommand;

import java.util.List;
import java.util.UUID;

public interface TemplateApplicationService {

    List<TemplateSummaryResponse> list(CompanyId companyId, boolean includeArchived);

    TemplateResponse get(CompanyId companyId, UUID id);

    TemplateResponse create(CompanyId companyId, CreateTemplateCommand command);

    TemplateResponse update(CompanyId companyId, UUID id, UpdateTemplateCommand command);

    TemplateResponse replaceDocument(CompanyId companyId, UUID id, ReplaceDocumentCommand command);

    TemplateResponse duplicate(CompanyId companyId, UUID id);

    TemplateResponse archive(CompanyId companyId, UUID id);

    TemplateResponse restore(CompanyId companyId, UUID id);

    PageImage page(CompanyId companyId, UUID id, int page);
}
