package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.service.domain.dto.CopyEntriesCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryCommand;
import com.bradox.erp.timesheet.service.domain.dto.EntryListResponse;
import com.bradox.erp.timesheet.service.domain.dto.EntryResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EntryApplicationService {

    /**
     * @param all when true lists every employee's entries and needs {@code tsh.entry.view_all};
     *            otherwise only the caller's (or {@code employeeId}'s, which also needs view_all).
     */
    EntryListResponse list(CompanyId companyId, UUID employeeId, boolean all, LocalDate from, LocalDate to,
                           UUID projectId, UUID taskId, Boolean billable);

    /** Time logged on one record (TSH-09 #2); own entries unless the caller may view all. */
    EntryListResponse byRecord(CompanyId companyId, String model, UUID recordId);

    EntryResponse get(CompanyId companyId, UUID id);

    EntryResponse log(CompanyId companyId, EntryCommand command);

    EntryResponse update(CompanyId companyId, UUID id, EntryCommand command);

    void delete(CompanyId companyId, UUID id);

    List<EntryResponse> copy(CompanyId companyId, CopyEntriesCommand command);
}
