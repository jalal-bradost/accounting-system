package com.bradox.erp.sign.domain.core.model;

import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A PDF with boxes placed on it. Signing it fills the boxes and stores a signed copy; the template itself never changes
 * by signing.
 */
public record SignTemplate(UUID id, UUID companyId, String name, UUID documentId, String documentSha256, int pageCount,
                           List<Field> fields, Instant createdAt) {

    public static final double MIN_SIZE = 0.01;

    /**
     * Where a box sits: page number (1-based) plus fractions (0 to 1) of the page width and height, measured from the
     * top-left of the page as displayed, so it does not depend on zoom or screen size.
     */
    public record Field(UUID id, int page, double x, double y, double width, double height, FieldType type) {

        public Field {
            if (type == null) {
                throw new SignDomainException("error.sign.field.type", null, "Unknown field type");
            }
            if (page < 1) {
                throw new SignDomainException("error.sign.field.page", null, "A field needs a page number of 1 or more");
            }
            if (!inUnit(x) || !inUnit(y) || !inUnit(width) || !inUnit(height)
                    || width < MIN_SIZE || height < MIN_SIZE || x + width > 1.0000001 || y + height > 1.0000001) {
                throw new SignDomainException("error.sign.field.bounds", null, "A field must lie inside its page and not be tiny");
            }
        }

        private static boolean inUnit(double v) {
            return !Double.isNaN(v) && v >= 0 && v <= 1;
        }
    }

    public SignTemplate {
        if (name == null || name.isBlank()) {
            throw new SignDomainException("error.sign.template.name", null, "A template needs a name");
        }
        name = name.trim();
        fields = fields == null ? List.of() : List.copyOf(fields);
        for (Field f : fields) {
            if (f.page() > pageCount) {
                throw new SignDomainException("error.sign.field.pageRange", new Object[]{pageCount},
                        "A field is on a page the document does not have (it has " + pageCount + " pages)");
            }
        }
    }

    public SignTemplate edit(String newName, List<Field> newFields) {
        return new SignTemplate(id, companyId, newName, documentId, documentSha256, pageCount, newFields, createdAt);
    }

    /** A template can be signed once it has somewhere to put the signature. */
    public void checkCanSign() {
        if (fields.stream().noneMatch(f -> f.type() == FieldType.SIGNATURE)) {
            throw new SignDomainException("error.sign.template.noSignature", null, "Add a signature box to the template first");
        }
    }
}
