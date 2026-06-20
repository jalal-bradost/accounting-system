package com.jalaldeveloper.accountingsystem.hr.domain.core.exception;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;

public class HrDomainException extends DomainException {

    public HrDomainException(String message) {
        super(message);
    }

    public HrDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
