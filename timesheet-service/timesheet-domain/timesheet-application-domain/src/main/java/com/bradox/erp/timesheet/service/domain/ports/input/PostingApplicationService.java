package com.bradox.erp.timesheet.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.service.domain.dto.BackfillResponse;
import com.bradox.erp.timesheet.service.domain.dto.PostingResponse;

import java.util.List;
import java.util.UUID;

/** Labor cost postings to the ledger (TSH-10). Everything here needs {@code tsh.posting.manage}. */
public interface PostingApplicationService {

    /** The posting behind a journal entry (its entry or its reversal), or null: lets the ledger link back to the week. */
    PostingResponse bySource(CompanyId companyId, UUID journalEntryId);

    /** TSH-10 integrity: approved weeks without a posting, postings of weeks no longer approved, and mismatched amounts. */
    com.bradox.erp.timesheet.service.domain.dto.IntegrityResponse integrity(CompanyId companyId);

    List<PostingResponse> list(CompanyId companyId, List<PostingStatus> statuses);

    /** Manual retry of a FAILED posting; idempotent on (week, version). */
    PostingResponse retry(CompanyId companyId, UUID postingId);

    /** Posts earlier approved weeks that have no posting, in date order, with a result per week (TSH-10 #12). */
    BackfillResponse backfill(CompanyId companyId);
}
