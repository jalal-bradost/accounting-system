package com.bradox.erp.timesheet.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.timesheet.domain.core.entity.Entry;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.rule.CostCalculator;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.service.domain.dto.ProfitabilityResponse;
import com.bradox.erp.timesheet.service.domain.dto.RecomputeCostResponse;
import com.bradox.erp.timesheet.service.domain.ports.input.CostApplicationService;
import com.bradox.erp.timesheet.service.domain.ports.output.LaborCostPort;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.EntryRepository;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
class CostApplicationServiceImpl implements CostApplicationService {

    private final ProjectRepository projects;
    private final EntryRepository entries;
    private final LaborCostPort laborCost;
    private final AuditLogPort audit;
    private final TimesheetAccess access;

    CostApplicationServiceImpl(ProjectRepository projects, EntryRepository entries, LaborCostPort laborCost,
                               AuditLogPort audit, TimesheetAccess access) {
        this.projects = projects;
        this.entries = entries;
        this.laborCost = laborCost;
        this.audit = audit;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitabilityResponse profitability(CompanyId companyId, UUID projectId) {
        access.require(TimesheetPermissions.COST_VIEW);
        Project p = projects.find(new ProjectId(projectId)).filter(x -> x.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        EntryRepository.ProjectTotals t = entries.projectTotals(companyId, projectId);
        return new ProfitabilityResponse(projectId, t.minutes(), t.billableMinutes(), p.getAllocatedMinutes(),
                t.costAmount(), t.missingCostEntries());
    }

    @Override
    @Transactional
    public RecomputeCostResponse recomputeMissing(CompanyId companyId) {
        access.require(TimesheetPermissions.COST_VIEW);
        int fixed = 0;
        int still = 0;
        // Only entries flagged "no cost rate" are touched; a non-zero snapshot is never rewritten (TSH-07 #7).
        for (Entry e : entries.findCostMissing(companyId)) {
            var rate = laborCost.hourlyRate(companyId, e.getEmployeeId(), e.getWorkDate()).orElse(null);
            if (rate == null) {
                still++;
                continue;
            }
            e.applyCost(rate.perHour(), rate.currency(), CostCalculator.cost(e.getMinutes(), rate.perHour()), false);
            entries.save(e);
            fixed++;
        }
        audit.recordBusinessEvent(companyId, "tsh.entry", companyId.getId(), "Missing costs recomputed",
                Map.of("recomputed", fixed, "stillMissing", still, "by", access.actorLabel()));
        return new RecomputeCostResponse(fixed, still);
    }
}
