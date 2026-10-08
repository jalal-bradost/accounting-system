package com.bradox.erp.documents.service.domain.ports.output;

import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Users and roles of the platform, as far as folder access needs them. */
public interface AccessSubjectPort {

    Set<UUID> roleIdsOf(UserId userId, CompanyId companyId);

    List<AccessSubjectResponse> listSubjects(CompanyId companyId);

    Map<UUID, String> names(SubjectType type, Collection<UUID> ids);
}
