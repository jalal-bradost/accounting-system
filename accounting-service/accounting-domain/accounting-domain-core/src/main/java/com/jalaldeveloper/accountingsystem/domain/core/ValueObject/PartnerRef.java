package com.jalaldeveloper.accountingsystem.domain.core.ValueObject;

import java.util.UUID;

/**
 * Optional reference from a journal entry / item to a partner in the contacts module.
 * Stored denormalized (id + name snapshot) so the accounting module never needs to
 * reach into contacts at read time and history stays correct if the partner is renamed.
 */
public record PartnerRef(UUID id, String name) {
    public static PartnerRef of(UUID id, String name) {
        return new PartnerRef(id, name);
    }
}
