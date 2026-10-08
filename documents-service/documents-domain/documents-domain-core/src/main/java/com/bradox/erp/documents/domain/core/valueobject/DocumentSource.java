package com.bradox.erp.documents.domain.core.valueobject;

/** Where a document came from. SIGN and GENERATED are set by other modules through the application service. */
public enum DocumentSource {
    UPLOAD,
    SIGN,
    GENERATED,
    API
}
