package com.bradox.erp.platform.star;

import java.util.Set;
import java.util.UUID;

/**
 * Records a user has starred (marked as a favourite), per company and model, e.g. model
 * {@code sales.order} or {@code inventory.product}. Stars belong to the user: nobody else sees them.
 */
public interface UserStarService {

    /** Ids of the records of {@code model} the user has starred. */
    Set<UUID> starred(UUID companyId, UUID userId, String model);

    /** Stars or unstars one record; doing it twice is harmless. */
    void setStarred(UUID companyId, UUID userId, String model, UUID recordId, boolean starred);
}
