package com.bradox.erp.integration;

import com.bradox.erp.documents.service.domain.dto.SystemDocumentCommand;
import com.bradox.erp.documents.service.domain.ports.input.SystemDocumentApplicationService;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.service.domain.ports.output.SignFilePort;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/** Sign's PDFs live in Documents, in two system folders: the template originals and the signed copies. */
@Component
public class SignFilesAdapter implements SignFilePort {

    private static final List<String> MANAGERS = List.of("ADMIN", "SIGN_MANAGER");

    private final SystemDocumentApplicationService system;

    public SignFilesAdapter(SystemDocumentApplicationService system) {
        this.system = system;
    }

    @Override
    public UUID storeTemplate(CompanyId companyId, UUID templateId, String fileName, byte[] pdf) {
        return system.store(companyId, new SystemDocumentCommand(fileName, new ByteArrayInputStream(pdf), "SIGN", "SIGN_TEMPLATES",
                "Sign templates", MANAGERS, "sign.template", templateId, null, null, "Sign"));
    }

    @Override
    public UUID storeSigned(CompanyId companyId, UUID signedDocumentId, String fileName, byte[] pdf) {
        return system.store(companyId, new SystemDocumentCommand(fileName, new ByteArrayInputStream(pdf), "SIGN", "SIGN",
                "Signed documents", MANAGERS, "sign.document", signedDocumentId, null, null, "Sign"));
    }

    @Override
    public byte[] read(CompanyId companyId, UUID documentId) {
        try (InputStream in = system.read(companyId, documentId, null).stream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new SignDomainException("error.sign.source.unreadable", null, "The document could not be read");
        }
    }
}
