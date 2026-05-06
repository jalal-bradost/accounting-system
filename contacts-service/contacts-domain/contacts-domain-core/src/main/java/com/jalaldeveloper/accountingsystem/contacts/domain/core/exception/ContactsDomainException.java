package com.jalaldeveloper.accountingsystem.contacts.domain.core.exception;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;

public class ContactsDomainException extends DomainException {

    public ContactsDomainException(String message) {
        super(message);
    }

    public ContactsDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
