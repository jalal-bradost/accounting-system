package com.jalaldeveloper.accountingsystem.hr.dataaccess.adapter;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.mapper.TimeOffDataAccessMapper;
import com.jalaldeveloper.accountingsystem.hr.dataaccess.repository.LeaveRequestJpaRepository;
import com.jalaldeveloper.accountingsystem.hr.domain.core.entity.LeaveRequest;
import com.jalaldeveloper.accountingsystem.hr.domain.core.valueobject.LeaveRequestId;
import com.jalaldeveloper.accountingsystem.hr.service.domain.ports.output.repository.LeaveRequestRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LeaveRequestRepositoryImpl implements LeaveRequestRepository {

    private final LeaveRequestJpaRepository jpaRepository;

    public LeaveRequestRepositoryImpl(LeaveRequestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LeaveRequest save(LeaveRequest request) {
        var existing = jpaRepository.findById(request.getId().getId()).orElse(null);
        return TimeOffDataAccessMapper.entityToDomain(
                jpaRepository.save(TimeOffDataAccessMapper.domainToEntity(request, existing)));
    }

    @Override
    public Optional<LeaveRequest> findById(LeaveRequestId id) {
        return jpaRepository.findById(id.getId()).map(TimeOffDataAccessMapper::entityToDomain);
    }

    @Override
    public List<LeaveRequest> search(CompanyId companyId, UUID employeeId, String state,
                                     LocalDate from, LocalDate to) {
        return jpaRepository.search(companyId.getId(), employeeId, state, from, to).stream()
                .map(TimeOffDataAccessMapper::entityToDomain)
                .toList();
    }

    @Override
    public BigDecimal sumValidatedDays(CompanyId companyId, UUID employeeId, UUID typeId,
                                       LocalDate from, LocalDate to) {
        BigDecimal sum = jpaRepository.sumValidatedDays(companyId.getId(), employeeId, typeId, from, to);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public long countPending(CompanyId companyId, UUID employeeId) {
        return jpaRepository.countByCompanyIdAndEmployeeIdAndState(
                companyId.getId(), employeeId, LeaveRequest.STATE_CONFIRM);
    }
}
