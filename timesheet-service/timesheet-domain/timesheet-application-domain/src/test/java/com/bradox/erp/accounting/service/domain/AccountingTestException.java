package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.domain.exception.DomainException;

/** Stands in for a failure raised by Accounting (closed period, missing journal) in the posting tests. */
public class AccountingTestException extends DomainException {
    public AccountingTestException(String message) {
        super(message);
    }
}
