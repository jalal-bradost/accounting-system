package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.UUID;

/** Sign keeps its PDFs in Documents: the template originals and the signed copies. */
public interface SignFilePort {

    UUID storeTemplate(CompanyId companyId, UUID templateId, String fileName, byte[] pdf);

    UUID storeSigned(CompanyId companyId, UUID signedDocumentId, String fileName, byte[] pdf);

    /** The current bytes of a stored document. */
    byte[] read(CompanyId companyId, UUID documentId);
}
