package com.jalaldeveloper.accountingsystem.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.accounting.service.domain.ports.output.repository.JournalRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.mapper.JournalDataAccessMapper;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalId;
import com.jalaldeveloper.accountingsystem.domain.core.entity.Journal;
import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JournalRepositoryImpl implements JournalRepository {

    private final JournalJpaRepository jpaRepository;
    private final JournalDataAccessMapper mapper;

    public JournalRepositoryImpl(JournalJpaRepository jpaRepository, JournalDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Journal save(Journal journal) {
        JournalEntity entity = mapper.domainToEntity(journal);
        JournalEntity saved = jpaRepository.save(entity);
        return mapper.entityToDomain(saved);
    }

    @Override
    public Optional<Journal> findById(JournalId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Optional<Journal> findByCompanyIdAndCode(CompanyId companyId, String code) {
        return jpaRepository.findByCompanyIdAndCode(companyId.getId(), code).map(mapper::entityToDomain);
    }

    @Override
    public List<Journal> findByCompanyId(CompanyId companyId) {
        return jpaRepository.findByCompanyId(companyId.getId()).stream()
                .map(mapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCompanyIdAndCode(CompanyId companyId, String code) {
        return jpaRepository.existsByCompanyIdAndCode(companyId.getId(), code);
    }
}
