package com.bradox.delin.purchase.domain.core;

import com.bradox.delin.domain.exception.DomainException;

public class PurchaseDomainException extends DomainException {

    public PurchaseDomainException(String message) {
        super(message);
    }

    public PurchaseDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
