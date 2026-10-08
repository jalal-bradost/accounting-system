package com.bradox.erp.documents.dataaccess.adapter;

import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.documents.service.domain.ports.output.AccessSubjectPort;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.dataaccess.entity.AppUserEntity;
import com.bradox.erp.platform.dataaccess.entity.RoleEntity;
import com.bradox.erp.platform.dataaccess.entity.UserRoleEntity;
import com.bradox.erp.platform.dataaccess.repository.AppUserJpaRepository;
import com.bradox.erp.platform.dataaccess.repository.RoleJpaRepository;
import com.bradox.erp.platform.dataaccess.repository.UserRoleJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Reads platform users and roles for folder access. Roles from other companies never count. */
@Component
public class AccessSubjectAdapter implements AccessSubjectPort {

    private static final int MAX_USERS = 1000;

    private final UserRoleJpaRepository userRoles;
    private final RoleJpaRepository roles;
    private final AppUserJpaRepository users;

    public AccessSubjectAdapter(UserRoleJpaRepository userRoles, RoleJpaRepository roles, AppUserJpaRepository users) {
        this.userRoles = userRoles;
        this.roles = roles;
        this.users = users;
    }

    @Override
    public Set<UUID> roleIdsOf(UserId userId, CompanyId companyId) {
        Set<UUID> roleIds = userRoles.findByUserId(userId.getId()).stream()
                .map(UserRoleEntity::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roles.findAllById(roleIds).stream()
                .filter(r -> companyId.getId().equals(r.getCompanyId()))
                .map(RoleEntity::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<AccessSubjectResponse> listSubjects(CompanyId companyId) {
        List<AccessSubjectResponse> out = new ArrayList<>();
        roles.findByCompanyId(companyId.getId()).stream()
                .sorted(Comparator.comparing(RoleEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(r -> out.add(new AccessSubjectResponse(SubjectType.ROLE.name(), r.getId(), r.getName())));
        users.search(companyId.getId(), "", true, PageRequest.of(0, MAX_USERS)).getContent().stream()
                .sorted(Comparator.comparing(AccessSubjectAdapter::userLabel, String.CASE_INSENSITIVE_ORDER))
                .forEach(u -> out.add(new AccessSubjectResponse(SubjectType.USER.name(), u.getId(), userLabel(u))));
        return out;
    }

    @Override
    public Map<UUID, String> names(SubjectType type, Collection<UUID> ids) {
        Map<UUID, String> out = new HashMap<>();
        if (ids.isEmpty()) {
            return out;
        }
        if (type == SubjectType.ROLE) {
            roles.findAllById(ids).forEach(r -> out.put(r.getId(), r.getName()));
        } else {
            users.findAllById(ids).forEach(u -> out.put(u.getId(), userLabel(u)));
        }
        return out;
    }

    private static String userLabel(AppUserEntity u) {
        String display = u.getDisplayName();
        return display != null && !display.isBlank() ? display : u.getUsername();
    }
}
