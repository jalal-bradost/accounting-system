package com.bradox.erp.expense.domain.core.exception;

import com.bradox.erp.domain.exception.DomainException;

public class ExpenseDomainException extends DomainException {

    public ExpenseDomainException(String message) {
        super(message);
    }

    public ExpenseDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExpenseDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
