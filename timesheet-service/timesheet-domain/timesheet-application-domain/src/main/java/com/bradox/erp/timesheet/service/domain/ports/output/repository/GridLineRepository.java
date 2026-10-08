package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.List;
import java.util.UUID;

/** Rows an employee added to their weekly grid by hand, kept even while empty (TSH-03 #4). */
public interface GridLineRepository {

    record Line(UUID projectId, UUID taskId) {
    }

    List<Line> list(CompanyId companyId, UUID employeeId);

    /** Idempotent. */
    void add(CompanyId companyId, UUID employeeId, UUID projectId, UUID taskId);

    void remove(CompanyId companyId, UUID employeeId, UUID projectId, UUID taskId);
}
