package com.bradox.erp.timesheet.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.timesheet.dataaccess.entity.ProjectEntity;
import com.bradox.erp.timesheet.dataaccess.mapper.TimesheetDataAccessMapper;
import com.bradox.erp.timesheet.dataaccess.repository.ProjectJpaRepository;
import com.bradox.erp.timesheet.domain.core.entity.Project;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectId;
import com.bradox.erp.timesheet.domain.core.valueobject.ProjectStatus;
import com.bradox.erp.timesheet.service.domain.ports.output.repository.ProjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProjectRepositoryImpl implements ProjectRepository {

    private final ProjectJpaRepository projects;
    private final TimesheetDataAccessMapper mapper;

    private final TransactionTemplate isolated;

    public ProjectRepositoryImpl(ProjectJpaRepository projects, TimesheetDataAccessMapper mapper,
                                 PlatformTransactionManager txManager) {
        this.projects = projects;
        this.mapper = mapper;
        this.isolated = new TransactionTemplate(txManager);
        this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Optional<Project> find(ProjectId id) {
        return projects.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public List<Project> findByIds(Collection<ProjectId> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return projects.findAllById(ids.stream().map(ProjectId::getId).toList()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Project> findAll(CompanyId companyId, boolean includeArchived) {
        List<ProjectEntity> rows = includeArchived ? projects.findByCompanyId(companyId.getId())
                : projects.findByCompanyIdAndStatus(companyId.getId(), ProjectStatus.ACTIVE.name());
        return rows.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Project> findBySystemKey(CompanyId companyId, String systemKey) {
        return projects.findByCompanyIdAndSystemKey(companyId.getId(), systemKey).map(mapper::toDomain);
    }

    @Override
    public boolean codeTaken(CompanyId companyId, String normalizedCode, ProjectId excludeId) {
        return excludeId == null
                ? projects.existsByCompanyIdAndCodeNormalized(companyId.getId(), normalizedCode)
                : projects.existsByCompanyIdAndCodeNormalizedAndIdNot(companyId.getId(), normalizedCode, excludeId.getId());
    }

    @Override
    public Project save(Project project) {
        ProjectEntity entity = projects.findById(project.getId().getId()).orElseGet(ProjectEntity::new);
        mapper.apply(project, entity);
        return mapper.toDomain(projects.saveAndFlush(entity));
    }

    @Override
    public Project createIfAbsent(Project project) {
        try {
            // Own transaction: a lost race must not mark the caller's transaction rollback-only.
            return isolated.execute(status -> save(project));
        } catch (DataIntegrityViolationException e) {
            return findBySystemKey(project.getCompanyId(), project.getSystemKey()).orElseThrow(() -> e);
        }
    }
}
