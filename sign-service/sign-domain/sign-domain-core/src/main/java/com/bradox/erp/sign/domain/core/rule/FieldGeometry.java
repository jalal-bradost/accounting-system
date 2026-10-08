package com.bradox.erp.sign.domain.core.rule;

import com.bradox.erp.sign.domain.core.exception.SignDomainException;

/**
 * Where a field sits: page number (1-based) plus fractions (0 to 1) of the page width and height, measured from the
 * top-left of the page as displayed (D5). Independent of zoom and screen size.
 */
public record FieldGeometry(int page, double x, double y, double width, double height) {

    public static final double MIN_SIZE = 0.01;

    public FieldGeometry {
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

    public boolean within(int pageCount) {
        return page <= pageCount;
    }
}
