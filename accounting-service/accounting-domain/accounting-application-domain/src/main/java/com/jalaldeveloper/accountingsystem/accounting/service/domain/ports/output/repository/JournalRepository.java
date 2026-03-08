package com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository;

import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for Journal persistence (hexagonal architecture).
 */
public interface JournalRepository {

    Journal save(Journal journal);

    Optional<Journal> findById(JournalId id);

    Optional<Journal> findByCompanyIdAndCode(CompanyId companyId, String code);

    List<Journal> findByCompanyId(CompanyId companyId);

    boolean existsByCompanyIdAndCode(CompanyId companyId, String code);
}
