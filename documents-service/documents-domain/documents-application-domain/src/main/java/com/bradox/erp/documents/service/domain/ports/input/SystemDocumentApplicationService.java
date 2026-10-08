package com.bradox.erp.documents.service.domain.ports.input;

import com.bradox.erp.documents.service.domain.dto.DocumentContent;
import com.bradox.erp.documents.service.domain.dto.SystemDocumentCommand;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.UUID;

/**
 * Entry points for other modules that act without a signed-in user, such as public signing. They bypass folder access
 * on purpose, so they must never be exposed through a controller.
 */
public interface SystemDocumentApplicationService {

    /** Stores a file in a system workspace, optionally linked to a record and locked against deletion. Returns its id. */
    UUID store(CompanyId companyId, SystemDocumentCommand command);

    /** Bytes of one version (the current one when {@code versionId} is null). Not audited per read. */
    DocumentContent read(CompanyId companyId, UUID documentId, UUID versionId);
}
