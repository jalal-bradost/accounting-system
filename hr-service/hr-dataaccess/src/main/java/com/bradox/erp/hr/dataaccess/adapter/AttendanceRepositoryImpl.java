package com.bradox.erp.hr.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.hr.dataaccess.mapper.AttendanceDataAccessMapper;
import com.bradox.erp.hr.dataaccess.repository.AttendanceJpaRepository;
import com.bradox.erp.hr.domain.core.entity.Attendance;
import com.bradox.erp.hr.domain.core.valueobject.AttendanceId;
import com.bradox.erp.hr.service.domain.ports.output.repository.AttendanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AttendanceRepositoryImpl implements AttendanceRepository {

    private final AttendanceJpaRepository jpaRepository;
    private final AttendanceDataAccessMapper mapper;

    public AttendanceRepositoryImpl(AttendanceJpaRepository jpaRepository,
                                    AttendanceDataAccessMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Attendance save(Attendance attendance) {
        var existing = jpaRepository.findById(attendance.getId().getId()).orElse(null);
        var toSave = mapper.domainToEntity(attendance, existing);
        return mapper.entityToDomain(jpaRepository.save(toSave));
    }

    @Override
    public Optional<Attendance> findById(AttendanceId id) {
        return jpaRepository.findById(id.getId()).map(mapper::entityToDomain);
    }

    @Override
    public Page<Attendance> search(CompanyId companyId,
                                   UUID employeeId,
                                   Instant from,
                                   Instant to,
                                   Pageable pageable) {
        return jpaRepository.search(companyId.getId(), employeeId, from, to, pageable)
                .map(mapper::entityToDomain);
    }

    @Override
    public List<Attendance> findForGantt(CompanyId companyId,
                                         UUID employeeId,
                                         Instant from,
                                         Instant to) {
        return jpaRepository.findForGantt(companyId.getId(), employeeId, from, to).stream()
                .map(mapper::entityToDomain)
                .toList();
    }
}
