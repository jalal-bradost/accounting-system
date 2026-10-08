package com.bradox.erp.timesheet.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PostingRepository {

    Optional<WeekPosting> find(PostingId id);

    /** The week's posting that is not REVERSED, if any. At most one may exist (BR-TSH-16). */
    Optional<WeekPosting> findActiveByWeek(WeekId weekId);

    int maxVersion(WeekId weekId);

    /** Newest posting per week, for showing the status on week rows. */
    Map<UUID, WeekPosting> findLatestByWeeks(Collection<WeekId> weekIds);

    List<WeekPosting> search(CompanyId companyId, Collection<PostingStatus> statuses);

    /** FAILED with attempts left, and PENDING older than {@code staleBefore}, for the hourly retry. */
    List<WeekPosting> findRetryable(Instant staleBefore);

    /** Approved weeks that have no active posting, oldest first, for backfill (TSH-10 #12). */
    List<WeekId> approvedWeeksWithoutActivePosting(CompanyId companyId);

    /** Sum of POSTED postings (not reversed) with an entry date in the range. */
    java.math.BigDecimal sumPosted(CompanyId companyId, java.time.LocalDate from, java.time.LocalDate to);

    /** The posting whose ledger entry, or whose reversal entry, is the given journal entry (journal-to-timesheet link). */
    Optional<WeekPosting> findByJournalEntry(CompanyId companyId, UUID journalEntryId);

    WeekPosting save(WeekPosting posting);
}
