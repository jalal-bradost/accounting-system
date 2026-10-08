package com.bradox.erp.documents.service.domain.ports.output.repository;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FolderRepository {

    Folder save(Folder folder);

    Optional<Folder> findById(FolderId id);

    /** All folders of a company, archived ones included. */
    List<Folder> findAll(CompanyId companyId);

    boolean existsAny(CompanyId companyId);

    Map<FolderId, List<FolderAccessGrant>> findGrants(CompanyId companyId);

    List<FolderAccessGrant> findGrants(FolderId folderId);

    void replaceGrants(CompanyId companyId, FolderId folderId, List<FolderAccessGrant> grants);
}
