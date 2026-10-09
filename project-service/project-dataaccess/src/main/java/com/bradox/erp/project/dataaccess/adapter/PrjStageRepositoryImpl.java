package com.bradox.erp.project.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.project.dataaccess.entity.PrjStageEntity;
import com.bradox.erp.project.dataaccess.repository.PrjStageJpaRepository;
import com.bradox.erp.project.domain.core.model.Stage;
import com.bradox.erp.project.service.domain.ports.output.StageRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PrjStageRepositoryImpl implements StageRepository {

    private final PrjStageJpaRepository jpa;

    public PrjStageRepositoryImpl(PrjStageJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Stage> findByProject(CompanyId companyId, UUID projectId) {
        return jpa.findByCompanyIdAndProjectIdOrderBySequenceAsc(companyId.getId(), projectId).stream()
                .map(PrjStageRepositoryImpl::toModel).toList();
    }

    @Override
    public Optional<Stage> find(CompanyId companyId, UUID id) {
        return jpa.findByCompanyIdAndId(companyId.getId(), id).map(PrjStageRepositoryImpl::toModel);
    }

    @Override
    public Stage save(Stage s) {
        PrjStageEntity e = jpa.findById(s.id()).orElseGet(PrjStageEntity::new);
        e.id = s.id();
        e.companyId = s.companyId();
        e.projectId = s.projectId();
        e.name = s.name();
        e.sequence = s.sequence();
        return toModel(jpa.save(e));
    }

    @Override
    public void delete(Stage s) {
        jpa.deleteById(s.id());
    }

    @Override
    public void deleteByProject(CompanyId companyId, UUID projectId) {
        jpa.deleteByProject(companyId.getId(), projectId);
    }

    private static Stage toModel(PrjStageEntity e) {
        return new Stage(e.id, e.companyId, e.projectId, e.name, e.sequence);
    }
}
