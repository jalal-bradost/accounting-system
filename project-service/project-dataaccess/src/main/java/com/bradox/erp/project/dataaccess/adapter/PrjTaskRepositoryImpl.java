package com.bradox.erp.project.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.project.dataaccess.entity.PrjTaskEntity;
import com.bradox.erp.project.dataaccess.repository.PrjTaskJpaRepository;
import com.bradox.erp.project.domain.core.model.Task;
import com.bradox.erp.project.service.domain.ports.output.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class PrjTaskRepositoryImpl implements TaskRepository {

    private final PrjTaskJpaRepository jpa;

    public PrjTaskRepositoryImpl(PrjTaskJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Task> findByProject(CompanyId companyId, UUID projectId) {
        return jpa.findByCompanyIdAndProjectIdOrderBySequenceAscCreatedAtAsc(companyId.getId(), projectId).stream()
                .map(PrjTaskRepositoryImpl::toModel).toList();
    }

    @Override
    public Optional<Task> find(CompanyId companyId, UUID id) {
        return jpa.findByCompanyIdAndId(companyId.getId(), id).map(PrjTaskRepositoryImpl::toModel);
    }

    @Override
    public Map<UUID, Long> countByProject(CompanyId companyId) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : jpa.countByProject(companyId.getId())) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public boolean existsInStage(CompanyId companyId, UUID stageId) {
        return jpa.existsByCompanyIdAndStageId(companyId.getId(), stageId);
    }

    @Override
    public Task save(Task t) {
        PrjTaskEntity e = jpa.findById(t.id()).orElseGet(PrjTaskEntity::new);
        e.id = t.id();
        e.companyId = t.companyId();
        e.projectId = t.projectId();
        e.stageId = t.stageId();
        e.name = t.name();
        e.description = t.description();
        e.customerPartnerId = t.customerPartnerId();
        e.assigneeUsername = t.assigneeUsername();
        e.deadline = t.deadline();
        e.priority = t.priority();
        e.sequence = t.sequence();
        e.createdAt = t.createdAt();
        return toModel(jpa.save(e));
    }

    @Override
    public void delete(Task t) {
        jpa.deleteById(t.id());
    }

    @Override
    public void deleteByProject(CompanyId companyId, UUID projectId) {
        jpa.deleteByProject(companyId.getId(), projectId);
    }

    private static Task toModel(PrjTaskEntity e) {
        return new Task(e.id, e.companyId, e.projectId, e.stageId, e.name, e.description, e.customerPartnerId, e.assigneeUsername,
                e.deadline, e.priority, e.sequence, e.createdAt);
    }
}
