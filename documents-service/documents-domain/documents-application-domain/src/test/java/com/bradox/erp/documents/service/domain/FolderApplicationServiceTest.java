package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.documents.service.domain.dto.CreateFolderCommand;
import com.bradox.erp.documents.service.domain.dto.FolderAccessResponse;
import com.bradox.erp.documents.service.domain.dto.FolderResponse;
import com.bradox.erp.documents.service.domain.dto.GrantCommand;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderAccessCommand;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderCommand;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderApplicationServiceTest extends ServiceTestBase {

    private final UUID adminRole = UUID.randomUUID();
    private final UUID hrRole = UUID.randomUUID();
    private final UUID accountantRole = UUID.randomUUID();

    private void roles() {
        subjects.subjects.add(new AccessSubjectResponse("ROLE", adminRole, "ADMIN"));
        subjects.subjects.add(new AccessSubjectResponse("ROLE", hrRole, "HR_MANAGER"));
        subjects.subjects.add(new AccessSubjectResponse("ROLE", accountantRole, "ACCOUNTANT"));
    }

    @Test
    void firstListSeedsDefaultWorkspacesWithHrAndFinanceClosed() {
        roles();
        asManager();
        List<FolderResponse> list = folderService.list(COMPANY, false);
        assertEquals(6, list.size());
        FolderResponse hr = list.stream().filter(f -> "HR".equals(f.systemKey())).findFirst().orElseThrow();
        FolderResponse sales = list.stream().filter(f -> "SALES".equals(f.systemKey())).findFirst().orElseThrow();
        assertTrue(hr.restricted());
        assertFalse(hr.inheritAccess());
        assertFalse(sales.restricted());
        assertEquals(6, folderService.list(COMPANY, false).size());
    }

    @Test
    void closedWorkspacesAreInvisibleToOrdinaryUsers() {
        roles();
        folderService.list(COMPANY, false);
        List<FolderResponse> visible = folderService.list(COMPANY, false);
        assertEquals(4, visible.size());
        assertTrue(visible.stream().noneMatch(f -> "HR".equals(f.systemKey()) || "FINANCE".equals(f.systemKey())));
        assertEquals("EDIT", visible.get(0).level());
    }

    @Test
    void seedingWithoutRolesLeavesEverythingOpen() {
        List<FolderResponse> list = folderService.list(COMPANY, false);
        assertEquals(6, list.size());
        assertTrue(list.stream().noneMatch(FolderResponse::restricted));
    }

    @Test
    void createAlsoSeedsAndEnforcesTreeRules() {
        FolderResponse created = folderService.create(COMPANY, new CreateFolderCommand(null, "Projects", null));
        assertEquals(7, folders.findAll(COMPANY).size());
        assertEquals(6, created.sequence());
        FolderResponse child = folderService.create(COMPANY, new CreateFolderCommand(created.id(), "Alpha", "hr.employee"));
        assertEquals("hr.employee", child.linkedModel());
        assertThrows(DocumentsDomainException.class,
                () -> folderService.create(COMPANY, new CreateFolderCommand(created.id(), " alpha ", null)));
    }

    @Test
    void readOnlyUsersCannotCreateOrTouchFolders() {
        permissions.remove("documents.document.write");
        Folder f = folder("Shared", null);
        ResponseStatusException top = assertThrows(ResponseStatusException.class,
                () -> folderService.create(COMPANY, new CreateFolderCommand(null, "New", null)));
        assertEquals(HttpStatus.FORBIDDEN, top.getStatusCode());
        ResponseStatusException sub = assertThrows(ResponseStatusException.class,
                () -> folderService.create(COMPANY, new CreateFolderCommand(f.getId().getId(), "New", null)));
        assertEquals(HttpStatus.FORBIDDEN, sub.getStatusCode());
        assertThrows(ResponseStatusException.class,
                () -> folderService.update(COMPANY, f.getId().getId(), new UpdateFolderCommand("X", null, null, null, null)));
    }

    @Test
    void cannotCreateUnderArchivedParent() {
        Folder f = folder("Old", null);
        f.archive();
        folders.save(f);
        assertThrows(DocumentsDomainException.class,
                () -> folderService.create(COMPANY, new CreateFolderCommand(f.getId().getId(), "Child", null)));
    }

    @Test
    void updateRenamesMovesAndRejectsCycles() {
        Folder a = folder("A", null);
        Folder b = folder("B", a.getId());
        FolderResponse renamed = folderService.update(COMPANY, a.getId().getId(), new UpdateFolderCommand("Alpha", null, null, 3, "x.y"));
        assertEquals("Alpha", renamed.name());
        assertEquals(3, renamed.sequence());
        assertThrows(DocumentsDomainException.class,
                () -> folderService.update(COMPANY, a.getId().getId(), new UpdateFolderCommand(null, b.getId().getId(), null, null, null)));
        FolderResponse moved = folderService.update(COMPANY, b.getId().getId(), new UpdateFolderCommand(null, null, true, null, null));
        assertEquals(null, moved.parentId());
        assertThrows(ResponseStatusException.class,
                () -> folderService.update(COMPANY, UUID.randomUUID(), new UpdateFolderCommand("x", null, null, null, null)));
    }

    @Test
    void archiveAndRestore() {
        Folder a = folder("A", null);
        assertTrue(folderService.archive(COMPANY, a.getId().getId()).archived());
        assertFalse(folderService.unarchive(COMPANY, a.getId().getId()).archived());
        assertEquals(0, folderService.list(COMPANY, false).stream().filter(FolderResponse::archived).count());
    }

    @Test
    void accessNeedsManageAndAtLeastOneManager() {
        roles();
        Folder f = folder("Contracts", null);
        // an ordinary editor has EDIT on an open folder, not MANAGE
        ResponseStatusException denied = assertThrows(ResponseStatusException.class, () -> folderService.getAccess(COMPANY, f.getId().getId()));
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());

        asManager();
        FolderAccessResponse open = folderService.getAccess(COMPANY, f.getId().getId());
        assertTrue(open.inheritAccess());
        assertFalse(open.restricted());

        assertThrows(DocumentsDomainException.class, () -> folderService.updateAccess(COMPANY, f.getId().getId(),
                new UpdateFolderAccessCommand(false, List.of(new GrantCommand(SubjectType.ROLE, hrRole, AccessLevel.VIEW)))));

        FolderAccessResponse closed = folderService.updateAccess(COMPANY, f.getId().getId(),
                new UpdateFolderAccessCommand(false, List.of(
                        new GrantCommand(SubjectType.ROLE, hrRole, AccessLevel.MANAGE),
                        new GrantCommand(SubjectType.ROLE, hrRole, AccessLevel.VIEW))));
        assertFalse(closed.inheritAccess());
        assertTrue(closed.restricted());
        assertEquals(1, closed.grants().size());
        assertEquals("HR_MANAGER", closed.grants().get(0).subjectName());

        FolderAccessResponse reopened = folderService.updateAccess(COMPANY, f.getId().getId(), new UpdateFolderAccessCommand(true, null));
        assertTrue(reopened.inheritAccess());
        assertEquals(3, folderService.listAccessSubjects(COMPANY).size());
    }

    @Test
    void childOfClosedFolderReportsWhichFolderGovernsIt() {
        asManager();
        Folder parent = folder("Parent", null);
        Folder child = folder("Child", parent.getId());
        folderService.updateAccess(COMPANY, parent.getId().getId(),
                new UpdateFolderAccessCommand(false, List.of(new GrantCommand(SubjectType.ROLE, hrRole, AccessLevel.MANAGE))));
        FolderAccessResponse access = folderService.getAccess(COMPANY, child.getId().getId());
        assertTrue(access.restricted());
        assertEquals(parent.getId().getId(), access.governedByFolderId());
    }

    @Test
    void userWithMatchingRoleSeesClosedFolder() {
        roles();
        Folder hr = restrictedFolder("HR", hrRole, AccessLevel.EDIT);
        assertEquals(0, folderService.list(COMPANY, false).stream().filter(f -> f.id().equals(hr.getId().getId())).count());
        subjects.rolesByUser.put(userId, Set.of(hrRole));
        List<FolderResponse> list = folderService.list(COMPANY, false);
        assertEquals(1, list.stream().filter(f -> f.id().equals(hr.getId().getId())).count());
        assertEquals("EDIT", list.stream().filter(f -> f.id().equals(hr.getId().getId())).findFirst().orElseThrow().level());
        assertEquals(new FolderId(hr.getId().getId()), hr.getId());
    }
}
