package com.bradox.erp.documents.service.domain.ports.output;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.Optional;
import java.util.UUID;

/**
 * Lets documents link to records of other modules without depending on them. The adapter lives in
 * the infrastructure module.
 */
public interface RecordLookupPort {

    boolean supports(String modelName);

    boolean exists(CompanyId companyId, String modelName, UUID recordId);

    /** A short display label such as an invoice number or an employee name. */
    Optional<String> label(CompanyId companyId, String modelName, UUID recordId);

    /** Permission a user needs to open records of this model; documents linked to it become visible to them. */
    Optional<String> readPermission(String modelName);
}
