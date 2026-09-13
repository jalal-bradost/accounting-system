package com.jalaldeveloper.accountingsystem.purchase.domain.core;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;

public class PurchaseDomainException extends DomainException {

    public PurchaseDomainException(String message) {
        super(message);
    }

    public PurchaseDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
