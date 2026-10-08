package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.ProjectRateEntity;
import com.bradox.erp.timesheet.dataaccess.repository.ProjectRateJpaRepository;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRateRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ProjectRateRepositoryImpl implements ProjectRateRepository {

    private final ProjectRateJpaRepository rates;

    public ProjectRateRepositoryImpl(ProjectRateJpaRepository rates) {
        this.rates = rates;
    }

    @Override
    public Map<UUID, UUID> find(ProjectId projectId) {
        Map<UUID, UUID> out = new LinkedHashMap<>();
        rates.findByProjectId(projectId.getId()).forEach(r -> out.put(r.getEmployeeId(), r.getSaleLineId()));
        return out;
    }

    @Override
    public Map<UUID, Map<UUID, UUID>> findForProjects(Collection<ProjectId> projectIds) {
        Map<UUID, Map<UUID, UUID>> out = new HashMap<>();
        if (projectIds.isEmpty()) {
            return out;
        }
        rates.findByProjectIdIn(projectIds.stream().map(ProjectId::getId).toList())
                .forEach(r -> out.computeIfAbsent(r.getProjectId(), k -> new LinkedHashMap<>()).put(r.getEmployeeId(), r.getSaleLineId()));
        return out;
    }

    @Override
    public void replace(CompanyId companyId, ProjectId projectId, Map<UUID, UUID> employeeToLine) {
        rates.deleteByProjectId(projectId.getId());
        rates.flush();
        employeeToLine.forEach((employee, line) -> {
            ProjectRateEntity e = new ProjectRateEntity();
            e.setId(UUID.randomUUID());
            e.setCompanyId(companyId.getId());
            e.setProjectId(projectId.getId());
            e.setEmployeeId(employee);
            e.setSaleLineId(line);
            rates.save(e);
        });
        rates.flush();
    }
}
