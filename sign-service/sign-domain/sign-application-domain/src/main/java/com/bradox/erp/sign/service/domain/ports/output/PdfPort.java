package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.service.domain.dto.PageImage;

import java.util.List;

/** All PDF and image work, behind one port so the domain stays free of PDF libraries (D3). */
public interface PdfPort {

    record PdfInfo(int pageCount) {
    }

    record Placement(int page, double x, double y, double width, double height, FieldType type, String text, Boolean bool,
                     byte[] image) {
    }

    record CertificateSigner(String name, String email, String role, String signedAtUtc, String signedAtLocal, String ip,
                             String channel) {
    }

    record Certificate(String reference, String documentName, String originalSha256, String contentSha256, String requestId,
                       String timeZone, String generatedAt, List<CertificateSigner> signers) {
    }

    /** Throws a domain exception for unreadable, encrypted, signed, too large or too long PDFs (BR-SIG-13). */
    PdfInfo inspect(byte[] pdf);

    /** One page as PNG, {@code scale} times 72 dpi. */
    PageImage render(byte[] pdf, int page, float scale);

    /** Decodes, validates and re-encodes a drawn or uploaded signature as a clean PNG (BR-SIG-10). */
    byte[] sanitizeSignatureImage(byte[] png);

    /** Draws the values on the original pages, flattened. */
    byte[] applyValues(byte[] sourcePdf, List<Placement> placements);

    /** Appends the certificate page. */
    byte[] appendCertificate(byte[] signedPdf, Certificate certificate);
}
