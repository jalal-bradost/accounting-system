package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.rule.AccessPolicy;
import com.bradox.erp.documents.domain.core.rule.FolderTree;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.service.domain.ports.output.AccessSubjectPort;
import com.bradox.erp.documents.service.domain.ports.output.repository.FolderRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the folder tree, grants and the current user once per request and answers "what can this
 * user do in this folder". Everything visible goes through here, so a folder the user may not see
 * answers 404 and never leaks that it exists.
 */
@Component
class DocumentAccessResolver {

    static final String PERM_WRITE = "documents.document.write";
    static final String PERM_ACCESS_MANAGE = "documents.access.manage";

    private final FolderRepository folders;
    private final AccessSubjectPort subjects;
    private final AuthorizationPort authorization;
    private final CompanyContext context;

    DocumentAccessResolver(FolderRepository folders, AccessSubjectPort subjects,
                           AuthorizationPort authorization, CompanyContext context) {
        this.folders = folders;
        this.subjects = subjects;
        this.authorization = authorization;
        this.context = context;
    }

    Access load(CompanyId companyId) {
        UserId user = context.currentUser().orElse(null);
        FolderTree tree = FolderTree.of(folders.findAll(companyId));
        Map<FolderId, List<FolderAccessGrant>> grants = folders.findGrants(companyId);
        boolean manager = authorization.hasAll(user, Set.of(PERM_ACCESS_MANAGE));
        AccessLevel openDefault = authorization.hasAll(user, Set.of(PERM_WRITE)) ? AccessLevel.EDIT : AccessLevel.VIEW;
        Set<java.util.UUID> roles = user == null ? Set.of() : subjects.roleIdsOf(user, companyId);
        AccessPolicy.Subject subject = new AccessPolicy.Subject(user == null ? null : user.getId(), roles, manager, openDefault);
        return new Access(tree, grants, subject, user, AccessPolicy.levelsForAll(tree, grants, subject));
    }

    boolean hasPermission(UserId user, String permission) {
        return authorization.hasAll(user, Set.of(permission));
    }

    /** Everything the services need to know about the current user's access in one company. */
    record Access(FolderTree tree, Map<FolderId, List<FolderAccessGrant>> grants, AccessPolicy.Subject subject,
                  UserId user, Map<FolderId, AccessLevel> levels) {

        AccessLevel level(FolderId id) {
            return id == null ? AccessLevel.NONE : levels.getOrDefault(id, AccessLevel.NONE);
        }

        boolean restricted(FolderId id) {
            return AccessPolicy.governingFolder(tree, id) != null;
        }

        /** 404 when the folder is invisible to the user, 403 when visible but not enough. */
        void require(FolderId id, AccessLevel needed) {
            AccessLevel have = level(id);
            if (have == AccessLevel.NONE) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
            }
            if (!have.atLeast(needed)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have " + needed + " access to this folder");
            }
        }

        /** The ids of folders the user can see, or null when every folder is visible (no filter needed). */
        Set<java.util.UUID> visibleFolderIds() {
            Set<java.util.UUID> out = new java.util.HashSet<>();
            boolean all = true;
            for (var e : levels.entrySet()) {
                if (e.getValue().atLeast(AccessLevel.VIEW)) {
                    out.add(e.getKey().getId());
                } else {
                    all = false;
                }
            }
            return all ? null : out;
        }
    }
}
