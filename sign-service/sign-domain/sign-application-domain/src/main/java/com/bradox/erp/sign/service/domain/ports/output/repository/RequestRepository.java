package com.bradox.erp.sign.service.domain.ports.output.repository;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.service.domain.dto.RequestFilter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository {

    Optional<SignRequest> find(CompanyId companyId, UUID id);

    /** By id alone, for background work that runs without a signed-in company. */
    Optional<SignRequest> findAny(UUID id);

    /** Across companies: a public signer arrives with only a link. */
    Optional<SignRequest> findByTokenHash(String tokenHash);

    PageResponse<SignRequest> search(CompanyId companyId, RequestFilter filter, UUID currentUserId);

    SignRequest save(SignRequest request);

    /** Drafts only; sent requests are never deleted. */
    void delete(CompanyId companyId, UUID id);

    /** Next reference such as SIGN/2026/0001, unique per company and year. */
    String nextReference(CompanyId companyId, int year);

    /** Live requests whose expiry has passed, across companies (daily job). */
    List<SignRequest> findLiveExpiredBefore(Instant now);

    /** Live requests that have a reminder interval (hourly check), across companies. */
    List<SignRequest> findLiveWithReminders();

    /** Requests where everyone signed and the final PDF is still missing, across companies (retry job). */
    List<SignRequest> findAwaitingFinal();

    List<SignRequest> findByRecord(CompanyId companyId, String recordModel, UUID recordId);

    /** Completed requests with no final document; the nightly orphan check. */
    List<SignRequest> findCompletedWithoutFinal();

    /** Counts for the dashboard. {@code userId} may be null for managers viewing everything. */
    long countWaitingForUser(CompanyId companyId, UUID userId);

    long countByStatus(CompanyId companyId, UUID createdByUserId, List<String> statuses);

    long countCompletedSince(CompanyId companyId, UUID createdByUserId, Instant since);

    long countExpiringBefore(CompanyId companyId, UUID createdByUserId, Instant before);

    Optional<SignRequest> findByFinalSha256(String sha256);
}
