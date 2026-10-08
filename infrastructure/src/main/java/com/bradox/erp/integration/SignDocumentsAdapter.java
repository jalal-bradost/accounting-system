package com.bradox.erp.integration;

import com.bradox.erp.documents.service.domain.dto.DocumentContent;
import com.bradox.erp.documents.service.domain.dto.SystemDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.VersionResponse;
import com.bradox.erp.documents.service.domain.ports.input.DocumentApplicationService;
import com.bradox.erp.documents.service.domain.ports.input.SystemDocumentApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.entity.SignRequestLimits;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.rule.Tokens;
import com.bradox.erp.sign.service.domain.ports.output.SignedDocumentSinkPort;
import com.bradox.erp.sign.service.domain.ports.output.SourceDocumentPort;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Sign's two doors into Documents: reading PDFs to sign, and storing the signed result (BR-SIG-12). */
@Component
public class SignDocumentsAdapter implements SourceDocumentPort, SignedDocumentSinkPort {

    static final String SIGN_FOLDER_KEY = "SIGN";
    static final String SIGN_FOLDER_NAME = "Signed documents";

    private final DocumentApplicationService documents;
    private final SystemDocumentApplicationService system;

    public SignDocumentsAdapter(DocumentApplicationService documents, SystemDocumentApplicationService system) {
        this.documents = documents;
        this.system = system;
    }

    @Override
    public SourceDoc readCurrent(CompanyId companyId, UUID documentId) {
        VersionResponse current = documents.versions(companyId, documentId).stream().filter(VersionResponse::current).findFirst()
                .orElseThrow(() -> new SignDomainException("error.sign.source.missing", null, "The document has no versions"));
        if (current.sizeBytes() > SignRequestLimits.MAX_PDF_BYTES) {
            throw new SignDomainException("error.sign.pdf.size", null, "The PDF must be at most 25 MB");
        }
        DocumentContent content = documents.content(companyId, documentId, current.id());
        byte[] bytes = readAll(content.stream());
        return new SourceDoc(documentId, current.id(), Tokens.sha256Hex(bytes), content.fileName(), bytes);
    }

    @Override
    public SourceDoc readPinned(CompanyId companyId, UUID documentId, UUID versionId) {
        DocumentContent content = system.read(companyId, documentId, versionId);
        byte[] bytes = readAll(content.stream());
        return new SourceDoc(documentId, versionId, Tokens.sha256Hex(bytes), content.fileName(), bytes);
    }

    @Override
    public UUID store(CompanyId companyId, String fileName, byte[] pdf, UUID requestId, String reference, String recordModel,
                      UUID recordId, LocalDate retentionUntil) {
        boolean hasRecord = recordModel != null && recordId != null;
        return system.store(companyId, new SystemDocumentCommand(fileName, new ByteArrayInputStream(pdf), "SIGN", SIGN_FOLDER_KEY,
                SIGN_FOLDER_NAME, List.of("ADMIN", "SIGN_MANAGER"), hasRecord ? recordModel : "sign.request",
                hasRecord ? recordId : requestId, retentionUntil, "Signed document " + reference, "Sign"));
    }

    @Override
    public byte[] read(CompanyId companyId, UUID documentId) {
        return readAll(system.read(companyId, documentId, null).stream());
    }

    private static byte[] readAll(InputStream in) {
        try (in) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new SignDomainException("error.sign.source.unreadable", null, "The document could not be read");
        }
    }
}
