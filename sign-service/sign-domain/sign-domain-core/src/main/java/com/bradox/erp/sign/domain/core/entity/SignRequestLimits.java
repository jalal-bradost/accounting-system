package com.bradox.erp.sign.domain.core.entity;

/** D7, BR-SIG-10, BR-SIG-13. */
public final class SignRequestLimits {

    public static final int DEFAULT_VALIDITY_DAYS = 30;
    public static final int MAX_VALIDITY_DAYS = 90;
    public static final int MAX_REMINDER_PROMPTS = 5;
    public static final int MAX_SIGNATURE_IMAGE_BYTES = 200 * 1024;
    public static final long MAX_PDF_BYTES = 25L * 1024 * 1024;
    public static final int MAX_PDF_PAGES = 200;
    public static final int MAX_TEXT_LENGTH = 500;
    public static final int MAX_BUILD_ATTEMPTS = 3;

    private SignRequestLimits() {
    }
}
