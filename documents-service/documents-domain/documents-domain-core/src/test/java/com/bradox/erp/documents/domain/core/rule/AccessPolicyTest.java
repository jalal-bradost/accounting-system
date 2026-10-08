package com.bradox.erp.documents.domain.core.rule;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessPolicyTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    private AccessPolicy.Subject user(boolean manager) {
        return new AccessPolicy.Subject(userId, Set.of(roleId), manager, AccessLevel.EDIT);
    }

    @Test
    void openChainGivesTheOpenDefault() {
        Folder root = FolderTreeRulesTest.folder(null, "Company");
        Folder child = FolderTreeRulesTest.folder(root.getId(), "Finance");
        FolderTree tree = FolderTree.of(List.of(root, child));
        assertNull(AccessPolicy.governingFolder(tree, child.getId()));
        assertEquals(AccessLevel.EDIT, AccessPolicy.effectiveLevel(tree, child.getId(), Map.of(), user(false)));
    }

    @Test
    void closedFolderOnlyShowsToMatchingUserOrRoleWithTheHighestLevel() {
        Folder root = FolderTreeRulesTest.folder(null, "Company");
        Folder hr = FolderTreeRulesTest.folder(root.getId(), "HR");
        hr.changeInheritAccess(false);
        Folder contracts = FolderTreeRulesTest.folder(hr.getId(), "Contracts");
        FolderTree tree = FolderTree.of(List.of(root, hr, contracts));

        Map<FolderId, List<FolderAccessGrant>> grants = Map.of(hr.getId(), List.of(
                new FolderAccessGrant(SubjectType.ROLE, roleId, AccessLevel.VIEW),
                new FolderAccessGrant(SubjectType.USER, userId, AccessLevel.EDIT),
                new FolderAccessGrant(SubjectType.USER, UUID.randomUUID(), AccessLevel.MANAGE)));

        assertEquals(hr.getId(), AccessPolicy.governingFolder(tree, contracts.getId()).getId());
        assertEquals(AccessLevel.EDIT, AccessPolicy.effectiveLevel(tree, contracts.getId(), grants, user(false)));

        AccessPolicy.Subject stranger = new AccessPolicy.Subject(UUID.randomUUID(), Set.of(), false, AccessLevel.EDIT);
        assertEquals(AccessLevel.NONE, AccessPolicy.effectiveLevel(tree, contracts.getId(), grants, stranger));

        AccessPolicy.Subject roleOnly = new AccessPolicy.Subject(UUID.randomUUID(), Set.of(roleId), false, AccessLevel.EDIT);
        assertEquals(AccessLevel.VIEW, AccessPolicy.effectiveLevel(tree, hr.getId(), grants, roleOnly));
    }

    @Test
    void managersAlwaysHaveManage() {
        Folder hr = FolderTreeRulesTest.folder(null, "HR");
        hr.changeInheritAccess(false);
        FolderTree tree = FolderTree.of(List.of(hr));
        assertEquals(AccessLevel.MANAGE, AccessPolicy.effectiveLevel(tree, hr.getId(), Map.of(), user(true)));
    }

    @Test
    void levelsForAllCoversEveryFolder() {
        Folder a = FolderTreeRulesTest.folder(null, "A");
        Folder b = FolderTreeRulesTest.folder(null, "B");
        b.changeInheritAccess(false);
        FolderTree tree = FolderTree.of(List.of(a, b));
        Map<FolderId, AccessLevel> levels = AccessPolicy.levelsForAll(tree, Map.of(), user(false));
        assertEquals(AccessLevel.EDIT, levels.get(a.getId()));
        assertEquals(AccessLevel.NONE, levels.get(b.getId()));
    }

    @Test
    void grantsNeedASubjectAndARealLevel() {
        assertThrows(DocumentsDomainException.class, () -> new FolderAccessGrant(null, userId, AccessLevel.VIEW));
        assertThrows(DocumentsDomainException.class, () -> new FolderAccessGrant(SubjectType.USER, null, AccessLevel.VIEW));
        assertThrows(DocumentsDomainException.class, () -> new FolderAccessGrant(SubjectType.USER, userId, AccessLevel.NONE));
        assertThrows(DocumentsDomainException.class, () -> new FolderAccessGrant(SubjectType.USER, userId, null));
    }

    @Test
    void accessLevelOrdering() {
        assertTrue(AccessLevel.MANAGE.atLeast(AccessLevel.EDIT));
        assertTrue(AccessLevel.EDIT.atLeast(AccessLevel.EDIT));
        assertTrue(!AccessLevel.VIEW.atLeast(AccessLevel.EDIT));
        assertEquals(AccessLevel.EDIT, AccessLevel.max(AccessLevel.VIEW, AccessLevel.EDIT));
        assertEquals(AccessLevel.EDIT, AccessLevel.max(AccessLevel.EDIT, AccessLevel.VIEW));
    }
}
