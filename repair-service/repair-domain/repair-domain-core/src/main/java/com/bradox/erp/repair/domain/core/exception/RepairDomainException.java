package com.bradox.erp.repair.domain.core.exception;

import com.bradox.erp.domain.exception.DomainException;

public class RepairDomainException extends DomainException {

    public RepairDomainException(String message) {
        super(message);
    }

    public RepairDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
