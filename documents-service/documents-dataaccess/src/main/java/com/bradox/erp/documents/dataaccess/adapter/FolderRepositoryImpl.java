package com.bradox.erp.documents.dataaccess.adapter;

import com.bradox.erp.documents.dataaccess.entity.FolderAccessEntity;
import com.bradox.erp.documents.dataaccess.entity.FolderEntity;
import com.bradox.erp.documents.dataaccess.mapper.DocumentsDataAccessMapper;
import com.bradox.erp.documents.dataaccess.repository.FolderAccessJpaRepository;
import com.bradox.erp.documents.dataaccess.repository.FolderJpaRepository;
import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.service.domain.ports.output.repository.FolderRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class FolderRepositoryImpl implements FolderRepository {

    private final FolderJpaRepository folders;
    private final FolderAccessJpaRepository access;
    private final DocumentsDataAccessMapper mapper;

    public FolderRepositoryImpl(FolderJpaRepository folders, FolderAccessJpaRepository access,
                                DocumentsDataAccessMapper mapper) {
        this.folders = folders;
        this.access = access;
        this.mapper = mapper;
    }

    @Override
    public Folder save(Folder folder) {
        FolderEntity entity = folders.findById(folder.getId().getId()).orElseGet(FolderEntity::new);
        mapper.apply(folder, entity);
        return mapper.toDomain(folders.saveAndFlush(entity));
    }

    @Override
    public Optional<Folder> findById(FolderId id) {
        return folders.findById(id.getId()).map(mapper::toDomain);
    }

    @Override
    public List<Folder> findAll(CompanyId companyId) {
        return folders.findByCompanyId(companyId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsAny(CompanyId companyId) {
        return folders.existsByCompanyId(companyId.getId());
    }

    @Override
    public Map<FolderId, List<FolderAccessGrant>> findGrants(CompanyId companyId) {
        Map<FolderId, List<FolderAccessGrant>> out = new HashMap<>();
        for (FolderAccessEntity e : access.findByCompanyId(companyId.getId())) {
            out.computeIfAbsent(new FolderId(e.getFolderId()), k -> new ArrayList<>()).add(mapper.toDomain(e));
        }
        return out;
    }

    @Override
    public List<FolderAccessGrant> findGrants(FolderId folderId) {
        return access.findByFolderId(folderId.getId()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void replaceGrants(CompanyId companyId, FolderId folderId, List<FolderAccessGrant> grants) {
        access.deleteAllForFolder(folderId.getId());
        access.saveAll(grants.stream().map(g -> mapper.toEntity(companyId.getId(), folderId.getId(), g)).toList());
        access.flush();
    }
}
