package com.bradox.erp.documents.service.domain.ports.input;

import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.documents.service.domain.dto.CreateFolderCommand;
import com.bradox.erp.documents.service.domain.dto.FolderAccessResponse;
import com.bradox.erp.documents.service.domain.dto.FolderResponse;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderAccessCommand;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderCommand;
import com.bradox.erp.domain.valueobject.CompanyId;

import java.util.List;
import java.util.UUID;

public interface FolderApplicationService {

    /** Folders the current user can see. Seeds the default workspaces the first time a company opens Documents. */
    List<FolderResponse> list(CompanyId companyId, boolean includeArchived);

    FolderResponse create(CompanyId companyId, CreateFolderCommand command);

    FolderResponse update(CompanyId companyId, UUID id, UpdateFolderCommand command);

    FolderResponse archive(CompanyId companyId, UUID id);

    FolderResponse unarchive(CompanyId companyId, UUID id);

    FolderAccessResponse getAccess(CompanyId companyId, UUID id);

    FolderAccessResponse updateAccess(CompanyId companyId, UUID id, UpdateFolderAccessCommand command);

    /** Users and roles that can be chosen in the access dialog. */
    List<AccessSubjectResponse> listAccessSubjects(CompanyId companyId);
}
