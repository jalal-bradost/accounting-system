package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.UUID;

/**
 * How another module (Repair first) gets time logged on its records (TSH-09). Called through an adapter in
 * {@code infrastructure}; no module imports another's dataaccess.
 */
public interface TimesheetTargetApplicationService {

    /** Idempotent: one task per (model, record), under a system-managed project for the model. Returns the task id. */
    UUID ensureTask(CompanyId companyId, String model, UUID recordId, String reference, UUID partnerId);

    /** The record was finished or canceled: its task becomes DONE or CANCELED and takes no more time. */
    void closeTask(CompanyId companyId, String model, UUID recordId, boolean canceled);

    /** The record was hard-deleted: its task is canceled, entries are kept. */
    void archiveTask(CompanyId companyId, String model, UUID recordId);
}
