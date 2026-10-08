package com.bradox.erp.sign.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.sign.dataaccess.entity.CounterEntity;
import com.bradox.erp.sign.dataaccess.entity.RequestEntity;
import com.bradox.erp.sign.dataaccess.entity.RequestFieldEntity;
import com.bradox.erp.sign.dataaccess.entity.SignerEntity;
import com.bradox.erp.sign.dataaccess.mapper.SignDataMapper;
import com.bradox.erp.sign.dataaccess.repository.CounterJpaRepository;
import com.bradox.erp.sign.dataaccess.repository.RequestFieldJpaRepository;
import com.bradox.erp.sign.dataaccess.repository.RequestJpaRepository;
import com.bradox.erp.sign.dataaccess.repository.SignerJpaRepository;
import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.service.domain.dto.RequestFilter;
import com.bradox.erp.sign.service.domain.ports.output.repository.RequestRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RequestRepositoryImpl implements RequestRepository {

    private static final List<String> LIVE = List.of("SENT", "IN_PROGRESS");

    private final RequestJpaRepository requests;
    private final SignerJpaRepository signers;
    private final RequestFieldJpaRepository fields;
    private final CounterJpaRepository counters;
    private final SignDataMapper mapper;

    public RequestRepositoryImpl(RequestJpaRepository requests, SignerJpaRepository signers, RequestFieldJpaRepository fields,
                                 CounterJpaRepository counters, SignDataMapper mapper) {
        this.requests = requests;
        this.signers = signers;
        this.fields = fields;
        this.counters = counters;
        this.mapper = mapper;
    }

    @Override
    public Optional<SignRequest> find(CompanyId companyId, UUID id) {
        return requests.findByCompanyIdAndId(companyId.getId(), id).map(e -> load(List.of(e)).get(0));
    }

    @Override
    public Optional<SignRequest> findAny(UUID id) {
        return requests.findById(id).map(e -> load(List.of(e)).get(0));
    }

    @Override
    public Optional<SignRequest> findByTokenHash(String tokenHash) {
        return signers.findByTokenHash(tokenHash).flatMap(s -> requests.findById(s.getRequestId())).map(e -> load(List.of(e)).get(0));
    }

    @Override
    public Optional<SignRequest> findByFinalSha256(String sha256) {
        return requests.findFirstByFinalSha256(sha256).map(e -> load(List.of(e)).get(0));
    }

    @Override
    public SignRequest save(SignRequest r) {
        requests.save(mapper.toEntity(r));
        replaceChildren(r);
        return r;
    }

    private void replaceChildren(SignRequest r) {
        Set<UUID> signerIds = r.getSigners().stream().map(s -> s.getId()).collect(Collectors.toSet());
        Set<UUID> fieldIds = r.getFields().stream().map(f -> f.getId()).collect(Collectors.toSet());
        List<SignerEntity> oldSigners = signers.findByRequestIdIn(List.of(r.getId()));
        List<RequestFieldEntity> oldFields = fields.findByRequestIdIn(List.of(r.getId()));
        fields.deleteAll(oldFields.stream().filter(f -> !fieldIds.contains(f.getId())).toList());
        signers.deleteAll(oldSigners.stream().filter(s -> !signerIds.contains(s.getId())).toList());
        signers.saveAll(r.getSigners().stream().map(s -> mapper.toEntity(r.getId(), s)).toList());
        fields.saveAll(r.getFields().stream().map(f -> mapper.toEntity(r.getId(), f)).toList());
    }

    @Override
    public void delete(CompanyId companyId, UUID id) {
        requests.findByCompanyIdAndId(companyId.getId(), id).ifPresent(e -> {
            fields.deleteByRequestId(id);
            signers.deleteByRequestId(id);
            requests.delete(e);
        });
    }

    @Override
    public String nextReference(CompanyId companyId, int year) {
        CounterEntity c = counters.lockFor(companyId.getId(), year)
                .orElseGet(() -> counters.saveAndFlush(new CounterEntity(companyId.getId(), year, 0)));
        c.setLastValue(c.getLastValue() + 1);
        counters.save(c);
        return String.format("SIGN/%d/%04d", year, c.getLastValue());
    }

    @Override
    public PageResponse<SignRequest> search(CompanyId companyId, RequestFilter f, UUID currentUserId) {
        Specification<RequestEntity> spec = baseSpec(companyId, currentUserId).and(filterSpec(f));
        int size = Math.max(1, Math.min(f.size() <= 0 ? 25 : f.size(), 200));
        Page<RequestEntity> page = requests.findAll(spec, PageRequest.of(Math.max(0, f.page()), size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(load(page.getContent()), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Specification<RequestEntity> baseSpec(CompanyId companyId, UUID userId) {
        return (root, q, cb) -> {
            Predicate company = cb.equal(root.get("companyId"), companyId.getId());
            if (userId == null) {
                return company;
            }
            Subquery<UUID> sq = q.subquery(UUID.class);
            Root<SignerEntity> s = sq.from(SignerEntity.class);
            sq.select(s.get("requestId")).where(cb.equal(s.get("requestId"), root.get("id")), cb.equal(s.get("userId"), userId));
            return cb.and(company, cb.or(cb.equal(root.get("createdByUserId"), userId), cb.exists(sq)));
        };
    }

    private Specification<RequestEntity> filterSpec(RequestFilter f) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.statuses() != null && !f.statuses().isEmpty()) {
                p.add(root.get("status").in(f.statuses()));
            }
            if (f.templateId() != null) {
                p.add(cb.equal(root.get("templateId"), f.templateId()));
            }
            if (f.recordModel() != null && !f.recordModel().isBlank()) {
                p.add(cb.equal(root.get("recordModel"), f.recordModel()));
            }
            if (f.recordId() != null) {
                p.add(cb.equal(root.get("recordId"), f.recordId()));
            }
            if (f.needsAttention()) {
                p.add(cb.isTrue(root.get("needsAttention")));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("reference")), like)));
            }
            if (f.from() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
            }
            if (f.to() != null) {
                p.add(cb.lessThan(root.get("createdAt"), f.to()));
            }
            if (f.signer() != null && !f.signer().isBlank()) {
                Subquery<UUID> sq = q.subquery(UUID.class);
                Root<SignerEntity> s = sq.from(SignerEntity.class);
                sq.select(s.get("requestId")).where(cb.equal(s.get("requestId"), root.get("id")),
                        cb.like(cb.lower(s.get("name")), "%" + f.signer().trim().toLowerCase() + "%"));
                p.add(cb.exists(sq));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<SignRequest> findLiveExpiredBefore(Instant now) {
        return load(requests.findByStatusInAndExpiresAtBefore(LIVE, now));
    }

    @Override
    public List<SignRequest> findLiveWithReminders() {
        return load(requests.findByStatusInAndReminderEveryDaysNotNull(LIVE));
    }

    @Override
    public List<SignRequest> findAwaitingFinal() {
        return load(requests.findAwaitingFinal());
    }

    @Override
    public List<SignRequest> findByRecord(CompanyId companyId, String recordModel, UUID recordId) {
        return load(requests.findByCompanyIdAndRecordModelAndRecordIdOrderByCreatedAtDesc(companyId.getId(), recordModel, recordId));
    }

    @Override
    public List<SignRequest> findCompletedWithoutFinal() {
        return load(requests.findByStatusAndFinalDocumentIdIsNull("COMPLETED"));
    }

    @Override
    public long countWaitingForUser(CompanyId companyId, UUID userId) {
        return requests.countWaitingForUser(companyId.getId(), userId);
    }

    @Override
    public long countByStatus(CompanyId companyId, UUID createdByUserId, List<String> statuses) {
        return requests.count(scope(companyId, createdByUserId).and((root, q, cb) -> root.get("status").in(statuses)));
    }

    @Override
    public long countCompletedSince(CompanyId companyId, UUID createdByUserId, Instant since) {
        return requests.count(scope(companyId, createdByUserId).and((root, q, cb) -> cb.and(
                cb.equal(root.get("status"), "COMPLETED"), cb.greaterThanOrEqualTo(root.get("completedAt"), since))));
    }

    @Override
    public long countExpiringBefore(CompanyId companyId, UUID createdByUserId, Instant before) {
        return requests.count(scope(companyId, createdByUserId).and((root, q, cb) -> cb.and(root.get("status").in(LIVE),
                cb.lessThanOrEqualTo(root.get("expiresAt"), before))));
    }

    private Specification<RequestEntity> scope(CompanyId companyId, UUID createdBy) {
        return (root, q, cb) -> createdBy == null ? cb.equal(root.get("companyId"), companyId.getId())
                : cb.and(cb.equal(root.get("companyId"), companyId.getId()), cb.equal(root.get("createdByUserId"), createdBy));
    }

    private List<SignRequest> load(List<RequestEntity> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = rows.stream().map(RequestEntity::getId).collect(Collectors.toSet());
        Map<UUID, List<SignerEntity>> signerRows = signers.findByRequestIdIn(ids).stream().collect(Collectors.groupingBy(SignerEntity::getRequestId));
        Map<UUID, List<RequestFieldEntity>> fieldRows = fields.findByRequestIdIn(ids).stream().collect(Collectors.groupingBy(RequestFieldEntity::getRequestId));
        return rows.stream().map(e -> mapper.toDomain(e, signerRows.getOrDefault(e.getId(), List.of()),
                fieldRows.getOrDefault(e.getId(), List.of()))).toList();
    }
}
