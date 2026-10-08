package com.bradox.erp.documents.domain.core.exception;

import com.bradox.erp.domain.exception.DomainException;

public class DocumentsDomainException extends DomainException {

    public DocumentsDomainException(String message) {
        super(message);
    }

    public DocumentsDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
