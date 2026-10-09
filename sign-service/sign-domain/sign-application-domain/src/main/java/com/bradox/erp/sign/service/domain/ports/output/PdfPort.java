package com.bradox.erp.sign.service.domain.ports.output;

import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.service.domain.dto.PageImage;

import java.util.List;

/** All PDF and image work, behind one port so the domain stays free of PDF libraries. */
public interface PdfPort {

    record PdfInfo(int pageCount) {
    }

    /** {@code text} for text boxes, {@code image} (PNG) for signatures. */
    record Placement(int page, double x, double y, double width, double height, FieldType type, String text, byte[] image) {
    }

    /** Throws a domain exception for unreadable, encrypted, digitally signed, too large or too long PDFs. */
    PdfInfo inspect(byte[] pdf);

    /** One page as PNG, {@code scale} times 72 dpi. */
    PageImage render(byte[] pdf, int page, float scale);

    /** Decodes, validates and re-encodes a drawn signature as a clean PNG. */
    byte[] sanitizeSignatureImage(byte[] png);

    /** Draws the values on the original pages, flattened. */
    byte[] applyValues(byte[] sourcePdf, List<Placement> placements);
}
