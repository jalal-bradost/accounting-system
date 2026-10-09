package com.bradox.erp.sign.domain.core.model;

public final class SignLimits {

    public static final long MAX_PDF_BYTES = 25L * 1024 * 1024;
    public static final int MAX_PDF_PAGES = 200;
    public static final int MAX_SIGNATURE_IMAGE_BYTES = 200 * 1024;
    public static final int MAX_TEXT_LENGTH = 500;

    private SignLimits() {
    }
}
