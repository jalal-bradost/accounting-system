package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.GridLineEntity;
import com.bradox.erp.timesheet.dataaccess.repository.GridLineJpaRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.GridLineRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GridLineRepositoryImpl implements GridLineRepository {

    private final GridLineJpaRepository lines;

    public GridLineRepositoryImpl(GridLineJpaRepository lines) {
        this.lines = lines;
    }

    @Override
    public List<Line> list(CompanyId companyId, UUID employeeId) {
        return lines.findByCompanyIdAndEmployeeId(companyId.getId(), employeeId).stream()
                .map(l -> new Line(l.getProjectId(), l.getTaskId())).toList();
    }

    @Override
    public void add(CompanyId companyId, UUID employeeId, UUID projectId, UUID taskId) {
        String key = GridLineEntity.keyOf(projectId, taskId);
        if (lines.findByEmployeeIdAndLineKey(employeeId, key).isPresent()) {
            return;
        }
        GridLineEntity e = new GridLineEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(companyId.getId());
        e.setEmployeeId(employeeId);
        e.setProjectId(projectId);
        e.setTaskId(taskId);
        e.setLineKey(key);
        lines.saveAndFlush(e);
    }

    @Override
    public void remove(CompanyId companyId, UUID employeeId, UUID projectId, UUID taskId) {
        lines.findByEmployeeIdAndLineKey(employeeId, GridLineEntity.keyOf(projectId, taskId))
                .ifPresent(lines::delete);
        lines.flush();
    }
}
