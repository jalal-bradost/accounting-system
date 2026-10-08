package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.WeekEntity;
import com.bradox.erp.timesheet.dataaccess.mapper.TimesheetDataAccessMapper;
import com.bradox.erp.timesheet.dataaccess.repository.WeekJpaRepository;
import com.bradox.erp.timesheet.domain.core.entity.TimesheetWeek;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekId;
import com.bradox.erp.timesheet.domain.core.valueobject.WeekStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.WeekRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WeekRepositoryImpl implements WeekRepository {

    private final WeekJpaRepository weeks;
    private final TimesheetDataAccessMapper mapper;

    public WeekRepositoryImpl(WeekJpaRepository weeks, TimesheetDataAccessMapper mapper) {
        this.weeks = weeks;
        this.mapper = mapper;
    }

    @Override
    public Optional<TimesheetWeek> find(CompanyId companyId, UUID employeeId, LocalDate weekStart) {
        return weeks.findByCompanyIdAndEmployeeIdAndWeekStart(companyId.getId(), employeeId, weekStart)
                .map(mapper::toDomain);
    }

    @Override
    public List<TimesheetWeek> findByIds(Collection<WeekId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return weeks.findAllById(ids.stream().map(WeekId::getId).toList()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<TimesheetWeek> findById(WeekId id) {
        return weeks.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public List<TimesheetWeek> search(CompanyId companyId, Collection<WeekStatus> statuses, Collection<UUID> employeeIds,
                                      LocalDate from, LocalDate to) {
        Specification<WeekEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("companyId"), companyId.getId()));
            if (statuses != null && !statuses.isEmpty()) {
                p.add(root.get("status").in(statuses.stream().map(Enum::name).toList()));
            }
            if (employeeIds != null) {
                p.add(root.get("employeeId").in(employeeIds));
            }
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("weekStart"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("weekStart"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return weeks.findAll(spec, Sort.by(Sort.Order.desc("weekStart"))).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TimesheetWeek> findForEmployees(CompanyId companyId, LocalDate weekStart, Collection<UUID> employeeIds) {
        if (employeeIds.isEmpty()) {
            return List.of();
        }
        return weeks.findByCompanyIdAndWeekStartAndEmployeeIdIn(companyId.getId(), weekStart, employeeIds).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public int lockStartedBefore(CompanyId companyId, LocalDate cutoff) {
        return weeks.lockStartedBefore(companyId.getId(), cutoff);
    }

    @Override
    public TimesheetWeek save(TimesheetWeek week) {
        WeekEntity entity = weeks.findById(week.getId().getId()).orElseGet(WeekEntity::new);
        mapper.apply(week, entity);
        return mapper.toDomain(weeks.saveAndFlush(entity));
    }
}
