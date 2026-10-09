package com.bradox.erp.project.dataaccess.adapter;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.project.dataaccess.entity.PrjProjectEntity;
import com.bradox.erp.project.dataaccess.repository.PrjProjectJpaRepository;
import com.bradox.erp.project.domain.core.model.Project;
import com.bradox.erp.project.service.domain.ports.output.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PrjProjectRepositoryImpl implements ProjectRepository {

    private final PrjProjectJpaRepository jpa;

    public PrjProjectRepositoryImpl(PrjProjectJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Project> findAll(CompanyId companyId) {
        return jpa.findByCompanyIdOrderByNameAsc(companyId.getId()).stream().map(PrjProjectRepositoryImpl::toModel).toList();
    }

    @Override
    public Optional<Project> find(CompanyId companyId, UUID id) {
        return jpa.findByCompanyIdAndId(companyId.getId(), id).map(PrjProjectRepositoryImpl::toModel);
    }

    @Override
    public Project save(Project p) {
        PrjProjectEntity e = jpa.findById(p.id()).orElseGet(PrjProjectEntity::new);
        e.id = p.id();
        e.companyId = p.companyId();
        e.name = p.name();
        e.customerPartnerId = p.customerPartnerId();
        e.managerUsername = p.managerUsername();
        e.startDate = p.startDate();
        e.endDate = p.endDate();
        e.color = p.color();
        e.createdAt = p.createdAt();
        return toModel(jpa.save(e));
    }

    @Override
    public void delete(Project p) {
        jpa.deleteById(p.id());
    }

    private static Project toModel(PrjProjectEntity e) {
        return new Project(e.id, e.companyId, e.name, e.customerPartnerId, e.managerUsername, e.startDate, e.endDate, e.color,
                e.createdAt);
    }
}
