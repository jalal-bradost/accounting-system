package com.bradox.erp.domain.core.exception;

import com.bradox.erp.domain.exception.DomainException;

public class AccountingDomainException extends DomainException {
    public AccountingDomainException(String message) {
        super(message);
    }

    public AccountingDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountingDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
