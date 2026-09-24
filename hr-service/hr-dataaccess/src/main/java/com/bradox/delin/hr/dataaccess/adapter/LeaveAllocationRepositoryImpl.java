package com.bradox.delin.hr.dataaccess.adapter;

import com.bradox.delin.domain.valueobject.CompanyId;
import com.bradox.delin.hr.dataaccess.mapper.TimeOffDataAccessMapper;
import com.bradox.delin.hr.dataaccess.repository.LeaveAllocationJpaRepository;
import com.bradox.delin.hr.domain.core.entity.LeaveAllocation;
import com.bradox.delin.hr.domain.core.valueobject.LeaveAllocationId;
import com.bradox.delin.hr.service.domain.ports.output.repository.LeaveAllocationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LeaveAllocationRepositoryImpl implements LeaveAllocationRepository {

    private final LeaveAllocationJpaRepository jpaRepository;

    public LeaveAllocationRepositoryImpl(LeaveAllocationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LeaveAllocation save(LeaveAllocation allocation) {
        var existing = jpaRepository.findById(allocation.getId().getId()).orElse(null);
        return TimeOffDataAccessMapper.entityToDomain(
                jpaRepository.save(TimeOffDataAccessMapper.domainToEntity(allocation, existing)));
    }

    @Override
    public Optional<LeaveAllocation> findById(LeaveAllocationId id) {
        return jpaRepository.findById(id.getId()).map(TimeOffDataAccessMapper::entityToDomain);
    }

    @Override
    public List<LeaveAllocation> search(CompanyId companyId, UUID employeeId) {
        return jpaRepository.search(companyId.getId(), employeeId).stream()
                .map(TimeOffDataAccessMapper::entityToDomain)
                .toList();
    }

    @Override
    public List<LeaveAllocation> findApproved(CompanyId companyId, UUID employeeId) {
        return jpaRepository.findByCompanyIdAndEmployeeIdAndState(
                        companyId.getId(), employeeId, LeaveAllocation.STATE_VALIDATE).stream()
                .map(TimeOffDataAccessMapper::entityToDomain)
                .toList();
    }
}
