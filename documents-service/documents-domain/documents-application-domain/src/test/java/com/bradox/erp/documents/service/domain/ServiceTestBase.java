package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.entity.FolderAccessGrant;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.SubjectType;
import com.bradox.erp.documents.service.domain.dto.DocumentResponse;
import com.bradox.erp.documents.service.domain.dto.UploadDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.UploadResultResponse;
import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.domain.valueobject.UserId;
import com.bradox.erp.platform.audit.AuditLogPort;
import com.bradox.erp.platform.security.AuthorizationPort;
import com.bradox.erp.platform.web.CompanyContext;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class ServiceTestBase {

    static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());

    final UUID userId = UUID.randomUUID();
    final Set<String> permissions = new HashSet<>(Set.of("documents.document.read", "documents.document.write"));
    Clock clock = Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneOffset.UTC);

    Fakes.FolderRepo folders;
    Fakes.DocRepo docs;
    Fakes.TagRepo tags;
    Fakes.Storage storage;
    Fakes.Subjects subjects;
    Fakes.Records records;
    DocumentAccessResolver resolver;
    CompanyContext ctx;
    AuditLogPort audit;
    FolderApplicationServiceImpl folderService;
    DocumentApplicationServiceImpl docService;
    TagApplicationServiceImpl tagService;

    @BeforeEach
    void setUpBase() {
        folders = new Fakes.FolderRepo();
        docs = new Fakes.DocRepo();
        tags = new Fakes.TagRepo();
        storage = new Fakes.Storage();
        subjects = new Fakes.Subjects();
        records = new Fakes.Records();
        ctx = mock(CompanyContext.class);
        when(ctx.currentUser()).thenAnswer(i -> Optional.of(new UserId(userId)));
        when(ctx.currentUserDisplay()).thenReturn("tester");
        audit = mock(AuditLogPort.class);
        AuthorizationPort auth = new AuthorizationPort() {
            @Override
            public boolean hasAll(UserId user, Set<String> required) {
                return permissions.containsAll(required);
            }

            @Override
            public boolean hasAny(UserId user, Set<String> required) {
                return required.stream().anyMatch(permissions::contains);
            }
        };
        resolver = new DocumentAccessResolver(folders, subjects, auth, ctx);
        folderService = new FolderApplicationServiceImpl(folders, resolver, subjects, audit, ctx, clock);
        docService = new DocumentApplicationServiceImpl(docs, tags, storage, records, resolver, audit, ctx, clock,
                1024 * 1024, 0);
        tagService = new TagApplicationServiceImpl(tags);
    }

    void asManager() {
        permissions.add("documents.access.manage");
    }

    Folder folder(String name, FolderId parent) {
        return folders.save(Folder.create(new FolderId(UUID.randomUUID()), COMPANY, parent, name, 0, null, null,
                "system", Instant.now(clock)));
    }

    /** A folder only the given role can see, at the given level. */
    Folder restrictedFolder(String name, UUID roleId, AccessLevel level) {
        Folder f = folder(name, null);
        f.changeInheritAccess(false);
        folders.save(f);
        folders.grants.put(f.getId(), List.of(new FolderAccessGrant(SubjectType.ROLE, roleId, level)));
        return f;
    }

    static byte[] pdf(String marker) {
        return ("%PDF-1.4\n" + marker + "\n%%EOF").getBytes(StandardCharsets.US_ASCII);
    }

    UploadDocumentCommand uploadCmd(FolderId folder, String fileName, byte[] bytes, boolean allowDuplicate) {
        return new UploadDocumentCommand(folder == null ? null : folder.getId(), fileName,
                new ByteArrayInputStream(bytes), allowDuplicate, null);
    }

    DocumentResponse uploadPdf(Folder folder, String name, String marker) {
        UploadResultResponse r = docService.upload(COMPANY, uploadCmd(folder.getId(), name, pdf(marker), false));
        return r.document();
    }
}
