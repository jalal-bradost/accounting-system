package com.bradox.erp.hr.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.hr.dataaccess.mapper.TimeOffDataAccessMapper;
import com.bradox.erp.hr.dataaccess.repository.TimeOffTypeJpaRepository;
import com.bradox.erp.hr.domain.core.entity.TimeOffType;
import com.bradox.erp.hr.domain.core.valueobject.TimeOffTypeId;
import com.bradox.erp.hr.service.domain.ports.output.repository.TimeOffTypeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TimeOffTypeRepositoryImpl implements TimeOffTypeRepository {

    private final TimeOffTypeJpaRepository jpaRepository;

    public TimeOffTypeRepositoryImpl(TimeOffTypeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TimeOffType save(TimeOffType type) {
        var existing = jpaRepository.findById(type.getId().getId()).orElse(null);
        return TimeOffDataAccessMapper.entityToDomain(
                jpaRepository.save(TimeOffDataAccessMapper.domainToEntity(type, existing)));
    }

    @Override
    public Optional<TimeOffType> findById(TimeOffTypeId id) {
        return jpaRepository.findById(id.getId()).map(TimeOffDataAccessMapper::entityToDomain);
    }

    @Override
    public List<TimeOffType> findByCompany(CompanyId companyId) {
        return jpaRepository.findByCompanyIdAndActiveTrueOrderBySortOrderAscNameAsc(companyId.getId()).stream()
                .map(TimeOffDataAccessMapper::entityToDomain)
                .toList();
    }

    @Override
    public boolean existsByCompanyAndCode(CompanyId companyId, String code) {
        return jpaRepository.existsByCompanyIdAndCode(companyId.getId(), code);
    }
}
