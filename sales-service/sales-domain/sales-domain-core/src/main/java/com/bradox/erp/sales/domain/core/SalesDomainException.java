package com.bradox.erp.sales.domain.core;

import com.bradox.erp.domain.exception.DomainException;

public class SalesDomainException extends DomainException {
    public SalesDomainException(String message) {
        super(message);
    }

    public SalesDomainException(String messageKey, Object[] messageArgs, String defaultMessage) {
        super(messageKey, messageArgs, defaultMessage);
    }
}
