package com.jalaldeveloper.accountingsystem.inventory.domain.core.exception;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;

/**
 * Thrown when an inventory aggregate rejects an operation because a domain invariant
 * (e.g. negative stock when not allowed, invalid state transition) would be violated.
 */
public class InventoryDomainException extends DomainException {

    public InventoryDomainException(String message) {
        super(message);
    }

    public InventoryDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
