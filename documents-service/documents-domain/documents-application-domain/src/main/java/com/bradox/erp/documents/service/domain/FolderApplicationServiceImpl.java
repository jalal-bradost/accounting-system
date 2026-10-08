package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.rule.AccessPolicy;
import com.bradox.erp.documents.domain.core.rule.FolderTree;
import com.bradox.erp.documents.domain.core.rule.FolderTreeRules;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.service.domain.DocumentAccessResolver.Access;
import com.bradox.erp.documents.service.domain.dto.AccessSubjectResponse;
import com.bradox.erp.documents.service.domain.dto.CreateFolderCommand;
import com.bradox.erp.documents.service.domain.dto.FolderAccessResponse;
import com.bradox.erp.documents.service.domain.dto.FolderResponse;
import com.bradox.erp.documents.service.domain.dto.GrantCommand;
import com.bradox.erp.documents.service.domain.dto.GrantResponse;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderAccessCommand;
import com.bradox.erp.documents.service.domain.dto.UpdateFolderCommand;
import com.bradox.erp.documents.service.domain.ports.input.FolderApplicationService;
import com.bradox.erp.documents.service.domain.ports.output.AccessSubjectPort;
import com.bradox.erp.documents.service.domain.ports.output.repository.FolderRepository;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
class FolderApplicationServiceImpl implements FolderApplicationService {

    static final String AUDIT_MODEL = "documents.folder";

    /** Workspaces created the first time a company opens Documents. */
    static final List<String[]> DEFAULT_WORKSPACES = List.of(
            new String[]{"INTERNAL", "Internal"},
            new String[]{"FINANCE", "Finance"},
            new String[]{"HR", "HR"},
            new String[]{"SALES", "Sales"},
            new String[]{"PURCHASE", "Purchase"},
            new String[]{"REPAIR", "Repair"});

    private final FolderRepository folders;
    private final DocumentAccessResolver access;
    private final AccessSubjectPort subjects;
    private final AuditLogPort audit;
    private final CompanyContext context;
    private final Clock clock;

    @Autowired
    FolderApplicationServiceImpl(FolderRepository folders, DocumentAccessResolver access, AccessSubjectPort subjects,
                                 AuditLogPort audit, CompanyContext context, ObjectProvider<Clock> clockProvider) {
        this(folders, access, subjects, audit, context, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    FolderApplicationServiceImpl(FolderRepository folders, DocumentAccessResolver access, AccessSubjectPort subjects,
                                 AuditLogPort audit, CompanyContext context, Clock clock) {
        this.folders = folders;
        this.access = access;
        this.subjects = subjects;
        this.audit = audit;
        this.context = context;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<FolderResponse> list(CompanyId companyId, boolean includeArchived) {
        if (!folders.existsAny(companyId)) {
            seedDefaults(companyId);
        }
        Access acc = access.load(companyId);
        return acc.tree().all().stream()
                .filter(f -> includeArchived || !f.isArchived())
                .filter(f -> acc.level(f.getId()).atLeast(AccessLevel.VIEW))
                .sorted(Comparator.comparingInt(Folder::getSequence).thenComparing(Folder::getNameNormalized))
                .map(f -> DocumentsMapper.folder(f, acc.level(f.getId()), acc.restricted(f.getId())))
                .toList();
    }

    @Override
    @Transactional
    public FolderResponse create(CompanyId companyId, CreateFolderCommand command) {
        if (!folders.existsAny(companyId)) {
            seedDefaults(companyId);
        }
        Access acc = access.load(companyId);
        FolderId parentId = command.parentId() == null ? null : new FolderId(command.parentId());
        if (parentId != null) {
            acc.require(parentId, AccessLevel.EDIT);
            if (acc.tree().get(parentId).isArchived()) {
                throw new DocumentsDomainException("error.documents.parentArchived", null, "The parent folder is archived");
            }
        } else if (!acc.subject().manager() && !acc.subject().openDefault().atLeast(AccessLevel.EDIT)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot create top-level folders");
        }
        FolderTreeRules.assertCanCreateUnder(acc.tree(), parentId, command.name());
        int sequence = acc.tree().all().stream()
                .filter(f -> parentId == null ? f.getParentId() == null : parentId.equals(f.getParentId()))
                .mapToInt(Folder::getSequence).max().orElse(-1) + 1;
        Folder folder = Folder.create(new FolderId(UUID.randomUUID()), companyId, parentId, command.name(), sequence,
                command.linkedModel(), null, context.currentUserDisplay(), Instant.now(clock));
        Folder saved = folders.save(folder);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, saved.getId().getId(), "Folder created",
                Map.of("name", saved.getName()));
        return toResponse(saved, access.load(companyId));
    }

    @Override
    @Transactional
    public FolderResponse update(CompanyId companyId, UUID id, UpdateFolderCommand command) {
        Access acc = access.load(companyId);
        Folder folder = loadFolder(acc, id);
        acc.require(folder.getId(), AccessLevel.EDIT);
        Map<String, Object> changes = new java.util.LinkedHashMap<>();

        boolean moving = Boolean.TRUE.equals(command.moveToRoot()) || command.parentId() != null;
        if (command.name() != null && !command.name().equals(folder.getName())) {
            FolderTreeRules.validateName(command.name());
            FolderTreeRules.assertUniqueSibling(acc.tree(), folder.getParentId(), command.name(), folder.getId());
            changes.put("name", command.name());
            folder.rename(command.name());
        }
        if (moving) {
            FolderId newParent = Boolean.TRUE.equals(command.moveToRoot()) ? null : new FolderId(command.parentId());
            if (newParent != null) {
                acc.require(newParent, AccessLevel.EDIT);
            }
            FolderTreeRules.assertCanMove(acc.tree(), folder, newParent);
            changes.put("parentId", newParent == null ? null : newParent.getId());
            folder.moveTo(newParent);
        }
        if (command.sequence() != null) {
            folder.changeSequence(command.sequence());
        }
        if (command.linkedModel() != null) {
            folder.changeLinkedModel(command.linkedModel());
        }
        Folder saved = folders.save(folder);
        if (!changes.isEmpty()) {
            audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Folder updated", changes);
        }
        return toResponse(saved, access.load(companyId));
    }

    @Override
    @Transactional
    public FolderResponse archive(CompanyId companyId, UUID id) {
        return setArchived(companyId, id, true);
    }

    @Override
    @Transactional
    public FolderResponse unarchive(CompanyId companyId, UUID id) {
        return setArchived(companyId, id, false);
    }

    private FolderResponse setArchived(CompanyId companyId, UUID id, boolean archived) {
        Access acc = access.load(companyId);
        Folder folder = loadFolder(acc, id);
        acc.require(folder.getId(), AccessLevel.EDIT);
        if (archived) {
            folder.archive();
        } else {
            folder.unarchive();
        }
        Folder saved = folders.save(folder);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, archived ? "Folder archived" : "Folder restored",
                Map.of("name", saved.getName()));
        return toResponse(saved, access.load(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public FolderAccessResponse getAccess(CompanyId companyId, UUID id) {
        Access acc = access.load(companyId);
        Folder folder = loadFolder(acc, id);
        acc.require(folder.getId(), AccessLevel.MANAGE);
        return accessResponse(acc, folder);
    }

    @Override
    @Transactional
    public FolderAccessResponse updateAccess(CompanyId companyId, UUID id, UpdateFolderAccessCommand command) {
        Access acc = access.load(companyId);
        Folder folder = loadFolder(acc, id);
        acc.require(folder.getId(), AccessLevel.MANAGE);

        List<GrantCommand> requested = command.grants() == null ? List.of() : command.grants();
        List<FolderAccessGrant> grants = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (GrantCommand g : requested) {
            if (seen.add(g.subjectType() + ":" + g.subjectId())) {
                grants.add(new FolderAccessGrant(g.subjectType(), g.subjectId(), g.level()));
            }
        }
        if (!command.inheritAccess() && grants.stream().noneMatch(g -> g.getLevel() == AccessLevel.MANAGE)) {
            throw new DocumentsDomainException("error.documents.needManager", null,
                    "A folder with its own access needs at least one user or role with Manage access");
        }
        folder.changeInheritAccess(command.inheritAccess());
        folders.save(folder);
        folders.replaceGrants(companyId, folder.getId(), grants);
        audit.recordBusinessEvent(companyId, AUDIT_MODEL, id, "Folder access changed",
                Map.of("inheritAccess", command.inheritAccess(), "grants", grants.size()));
        Access fresh = access.load(companyId);
        return accessResponse(fresh, fresh.tree().get(folder.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessSubjectResponse> listAccessSubjects(CompanyId companyId) {
        return subjects.listSubjects(companyId);
    }

    private FolderAccessResponse accessResponse(Access acc, Folder folder) {
        List<FolderAccessGrant> own = folders.findGrants(folder.getId());
        Map<UUID, String> userNames = subjects.names(SubjectType.USER,
                own.stream().filter(g -> g.getSubjectType() == SubjectType.USER).map(FolderAccessGrant::getSubjectId).toList());
        Map<UUID, String> roleNames = subjects.names(SubjectType.ROLE,
                own.stream().filter(g -> g.getSubjectType() == SubjectType.ROLE).map(FolderAccessGrant::getSubjectId).toList());
        List<GrantResponse> grants = own.stream()
                .map(g -> new GrantResponse(g.getSubjectType().name(), g.getSubjectId(),
                        (g.getSubjectType() == SubjectType.USER ? userNames : roleNames).get(g.getSubjectId()),
                        g.getLevel().name()))
                .toList();
        Folder governing = AccessPolicy.governingFolder(acc.tree(), folder.getId());
        return new FolderAccessResponse(folder.isInheritAccess(), governing != null,
                governing == null ? null : governing.getId().getId(),
                governing == null ? null : governing.getName(), grants);
    }

    private Folder loadFolder(Access acc, UUID id) {
        Folder folder = acc.tree().get(new FolderId(id));
        if (folder == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found");
        }
        return folder;
    }

    private FolderResponse toResponse(Folder folder, Access acc) {
        return DocumentsMapper.folder(folder, acc.level(folder.getId()), acc.restricted(folder.getId()));
    }

    /** Workspaces that start closed, with the roles that manage them. Others stay open until a manager changes them. */
    private static final Map<String, List<String>> RESTRICTED_WORKSPACES = Map.of(
            "HR", List.of("ADMIN", "HR_MANAGER"),
            "FINANCE", List.of("ADMIN", "ACCOUNTANT"));

    private void seedDefaults(CompanyId companyId) {
        int sequence = 0;
        Instant now = Instant.now(clock);
        Map<String, UUID> rolesByName = new java.util.HashMap<>();
        for (AccessSubjectResponse s : subjects.listSubjects(companyId)) {
            if ("ROLE".equals(s.type())) {
                rolesByName.put(s.name(), s.id());
            }
        }
        for (String[] ws : DEFAULT_WORKSPACES) {
            Folder folder = Folder.create(new FolderId(UUID.randomUUID()), companyId, null, ws[1], sequence++, null,
                    ws[0], "system", now);
            List<FolderAccessGrant> grants = new ArrayList<>();
            for (String roleName : RESTRICTED_WORKSPACES.getOrDefault(ws[0], List.of())) {
                UUID roleId = rolesByName.get(roleName);
                if (roleId != null) {
                    grants.add(new FolderAccessGrant(SubjectType.ROLE, roleId, AccessLevel.MANAGE));
                }
            }
            if (!grants.isEmpty()) {
                folder.changeInheritAccess(false);
            }
            Folder saved = folders.save(folder);
            if (!grants.isEmpty()) {
                folders.replaceGrants(companyId, saved.getId(), grants);
            }
        }
    }
}
