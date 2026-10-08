package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.WeekPostingEntity;
import com.bradox.erp.timesheet.dataaccess.entity.WeekPostingLineEntity;
import com.bradox.erp.timesheet.dataaccess.repository.WeekPostingJpaRepository;
import com.bradox.erp.timesheet.dataaccess.repository.WeekPostingLineJpaRepository;
import com.bradox.erp.timesheet.domain.core.entity.WeekPosting;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingId;
import com.bradox.erp.timesheet.domain.core.valueobject.PostingStatus;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.PostingRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PostingRepositoryImpl implements PostingRepository {

    private final WeekPostingJpaRepository postings;
    private final WeekPostingLineJpaRepository lines;

    public PostingRepositoryImpl(WeekPostingJpaRepository postings, WeekPostingLineJpaRepository lines) {
        this.postings = postings;
        this.lines = lines;
    }

    @Override
    public Optional<WeekPosting> find(PostingId id) {
        return postings.findById(id.getId()).map(e -> toDomain(e, linesOf(List.of(e.getId()))));
    }

    @Override
    public Optional<WeekPosting> findActiveByWeek(WeekId weekId) {
        return postings.findFirstByWeekIdAndStatusNot(weekId.getId(), PostingStatus.REVERSED.name())
                .map(e -> toDomain(e, linesOf(List.of(e.getId()))));
    }

    @Override
    public int maxVersion(WeekId weekId) {
        return postings.maxVersion(weekId.getId());
    }

    @Override
    public Map<UUID, WeekPosting> findLatestByWeeks(Collection<WeekId> weekIds) {
        Map<UUID, WeekPosting> out = new HashMap<>();
        if (weekIds.isEmpty()) {
            return out;
        }
        // Newest version first, so the first one seen per week is the latest.
        for (WeekPostingEntity e : postings.findByWeekIdInOrderByVersionDesc(weekIds.stream().map(WeekId::getId).toList())) {
            out.putIfAbsent(e.getWeekId(), toDomain(e, List.of()));
        }
        return out;
    }

    @Override
    public List<WeekPosting> search(CompanyId companyId, Collection<PostingStatus> statuses) {
        List<WeekPostingEntity> rows = statuses == null || statuses.isEmpty()
                ? postings.findByCompanyIdOrderByCreatedAtDesc(companyId.getId())
                : postings.findByCompanyIdAndStatusInOrderByCreatedAtDesc(companyId.getId(),
                        statuses.stream().map(Enum::name).toList());
        return hydrate(rows);
    }

    @Override
    public List<WeekPosting> findRetryable(Instant staleBefore) {
        return hydrate(postings.findRetryable(staleBefore));
    }

    @Override
    public List<WeekId> approvedWeeksWithoutActivePosting(CompanyId companyId) {
        return postings.approvedWeeksWithoutActivePosting(companyId.getId()).stream().map(WeekId::new).toList();
    }

    @Override
    public java.math.BigDecimal sumPosted(CompanyId companyId, java.time.LocalDate from, java.time.LocalDate to) {
        return postings.sumPosted(companyId.getId(), from, to);
    }

    @Override
    public Optional<WeekPosting> findByJournalEntry(CompanyId companyId, UUID journalEntryId) {
        return postings.findFirstByCompanyIdAndJournalEntryId(companyId.getId(), journalEntryId)
                .or(() -> postings.findFirstByCompanyIdAndReversalEntryId(companyId.getId(), journalEntryId))
                .map(e -> toDomain(e, linesOf(List.of(e.getId()))));
    }

    @Override
    public WeekPosting save(WeekPosting p) {
        WeekPostingEntity e = postings.findById(p.getId().getId()).orElseGet(WeekPostingEntity::new);
        e.setId(p.getId().getId());
        e.setCompanyId(p.getCompanyId().getId());
        e.setWeekId(p.getWeekId().getId());
        e.setVersion(p.getVersion());
        e.setStatus(p.getStatus().name());
        e.setJournalEntryId(p.getJournalEntryId());
        e.setReversalEntryId(p.getReversalEntryId());
        e.setEntryDate(p.getEntryDate());
        e.setLatePosted(p.isLatePosted());
        e.setTotalAmount(p.getTotalAmount());
        e.setAttempts(p.getAttempts());
        e.setErrorMessage(p.getErrorMessage());
        e.setCreatedAt(p.getCreatedAt());
        e.setPostedAt(p.getPostedAt());
        postings.saveAndFlush(e);
        if (!p.getLines().isEmpty()) {
            lines.deleteByPostingId(e.getId());
            lines.flush();
            for (WeekPosting.Line l : p.getLines()) {
                WeekPostingLineEntity le = new WeekPostingLineEntity();
                le.setId(UUID.randomUUID());
                le.setPostingId(e.getId());
                le.setProjectId(l.projectId());
                le.setDebitAccountId(l.debitAccountId());
                le.setAmount(l.amount());
                le.setMinutes(l.minutes());
                lines.save(le);
            }
            lines.flush();
        }
        return find(p.getId()).orElse(p);
    }

    private List<WeekPosting> hydrate(List<WeekPostingEntity> rows) {
        Map<UUID, List<WeekPostingLineEntity>> byPosting = linesOf(rows.stream().map(WeekPostingEntity::getId).toList());
        return rows.stream().map(r -> toDomain(r, byPosting)).toList();
    }

    private Map<UUID, List<WeekPostingLineEntity>> linesOf(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return lines.findByPostingIdIn(ids).stream().collect(Collectors.groupingBy(WeekPostingLineEntity::getPostingId));
    }

    private static WeekPosting toDomain(WeekPostingEntity e, Map<UUID, List<WeekPostingLineEntity>> lineMap) {
        return toDomain(e, lineMap.getOrDefault(e.getId(), List.of()));
    }

    private static WeekPosting toDomain(WeekPostingEntity e, List<WeekPostingLineEntity> lineRows) {
        return WeekPosting.restore(new PostingId(e.getId()), new CompanyId(e.getCompanyId()), new WeekId(e.getWeekId()),
                e.getVersion(), PostingStatus.valueOf(e.getStatus()), e.getJournalEntryId(), e.getReversalEntryId(),
                e.getEntryDate(), e.isLatePosted(), e.getTotalAmount(), e.getAttempts(), e.getErrorMessage(),
                e.getCreatedAt(), e.getPostedAt(), lineRows.stream().map(l -> new WeekPosting.Line(l.getProjectId(),
                        l.getDebitAccountId(), l.getAmount(), l.getMinutes())).toList());
    }
}
