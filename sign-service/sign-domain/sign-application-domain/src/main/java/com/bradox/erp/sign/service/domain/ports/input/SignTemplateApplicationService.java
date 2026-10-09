package com.bradox.erp.sign.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.SignCommand;
import com.bradox.erp.sign.service.domain.dto.SignedDocumentResponse;
import com.bradox.erp.sign.service.domain.dto.TemplateCommand;
import com.bradox.erp.sign.service.domain.dto.TemplateResponse;

import java.util.List;
import java.util.UUID;

public interface SignTemplateApplicationService {

    List<TemplateResponse> list(CompanyId companyId);

    TemplateResponse get(CompanyId companyId, UUID id);

    /** Uploads the PDF and creates a template with no boxes yet. */
    TemplateResponse create(CompanyId companyId, String name, String fileName, byte[] pdf);

    TemplateResponse update(CompanyId companyId, UUID id, TemplateCommand command);

    /** Removes the template; signed PDFs stay in Documents. */
    void delete(CompanyId companyId, UUID id);

    PageImage page(CompanyId companyId, UUID id, int page);

    SignedDocumentResponse sign(CompanyId companyId, UUID id, SignCommand command);

    List<SignedDocumentResponse> signedDocuments(CompanyId companyId, UUID templateId);

    FileContent download(CompanyId companyId, UUID signedDocumentId);
}
