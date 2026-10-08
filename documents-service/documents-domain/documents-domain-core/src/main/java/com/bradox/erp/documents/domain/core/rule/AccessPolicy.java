package com.bradox.erp.documents.domain.core.rule;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * BR-DOC-04 and DOC-07. A folder either inherits access from its parent or has its own grants.
 * A chain that never reaches a folder with its own grants is open: everyone who holds the global
 * document permission gets {@code openDefault}. A folder with its own grants is closed: only
 * matching users and roles see it, with the highest matching level. Document managers always
 * have MANAGE.
 */
public final class AccessPolicy {

    private AccessPolicy() {
    }

    /** The user whose access is being resolved. */
    public record Subject(UUID userId, Set<UUID> roleIds, boolean manager, AccessLevel openDefault) {
        public Subject {
            roleIds = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        }
    }

    /** The folder whose own grants govern {@code folderId}, or null when the chain is open. */
    public static Folder governingFolder(FolderTree tree, FolderId folderId) {
        Set<FolderId> seen = new HashSet<>();
        Folder cursor = tree.get(folderId);
        while (cursor != null && seen.add(cursor.getId())) {
            if (!cursor.isInheritAccess()) {
                return cursor;
            }
            cursor = cursor.getParentId() == null ? null : tree.get(cursor.getParentId());
        }
        return null;
    }

    public static AccessLevel effectiveLevel(FolderTree tree, FolderId folderId,
                                             Map<FolderId, List<FolderAccessGrant>> grants, Subject subject) {
        if (subject.manager()) {
            return AccessLevel.MANAGE;
        }
        Folder governing = governingFolder(tree, folderId);
        if (governing == null) {
            return subject.openDefault();
        }
        AccessLevel best = AccessLevel.NONE;
        for (FolderAccessGrant g : grants.getOrDefault(governing.getId(), List.of())) {
            boolean matches = g.getSubjectType() == SubjectType.USER
                    ? g.getSubjectId().equals(subject.userId())
                    : subject.roleIds().contains(g.getSubjectId());
            if (matches) {
                best = AccessLevel.max(best, g.getLevel());
            }
        }
        return best;
    }

    /** Effective level for every folder in the tree. */
    public static Map<FolderId, AccessLevel> levelsForAll(FolderTree tree,
                                                          Map<FolderId, List<FolderAccessGrant>> grants,
                                                          Subject subject) {
        Map<FolderId, AccessLevel> out = new HashMap<>();
        for (Folder f : tree.all()) {
            out.put(f.getId(), effectiveLevel(tree, f.getId(), grants, subject));
        }
        return out;
    }
}
