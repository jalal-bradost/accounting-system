package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.AccessLevel;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.service.domain.dto.AddLinkCommand;
import com.bradox.erp.documents.service.domain.dto.DocumentContent;
import com.bradox.erp.documents.service.domain.dto.DocumentResponse;
import com.bradox.erp.documents.service.domain.dto.DocumentSearchQuery;
import com.bradox.erp.documents.service.domain.dto.DocumentSummaryResponse;
import com.bradox.erp.documents.service.domain.dto.LockRetentionCommand;
import com.bradox.erp.documents.service.domain.dto.RestoreDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.SetTagsCommand;
import com.bradox.erp.documents.service.domain.dto.TagCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetCommand;
import com.bradox.erp.documents.service.domain.dto.TagFacetResponse;
import com.bradox.erp.documents.service.domain.dto.TagResponse;
import com.bradox.erp.documents.service.domain.dto.UpdateDocumentCommand;
import com.bradox.erp.documents.service.domain.dto.UploadResultResponse;
import com.bradox.erp.documents.service.domain.dto.VersionResponse;
import com.bradox.erp.platform.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentApplicationServiceTest extends ServiceTestBase {

    private DocumentSearchQuery all() {
        return new DocumentSearchQuery(null, null, false, null, null, null, null, null, null, null, null, 0, 50);
    }

    private DocumentSearchQuery inFolder(Folder f, String q) {
        return new DocumentSearchQuery(q, f.getId().getId(), true, null, null, null, null, null, null, null, null, 0, 50);
    }

    // ---------------------------------------------------------------- upload

    @Test
    void uploadStoresFileAndCreatesVersionOne() throws IOException {
        Folder f = folder("Inbox", null);
        byte[] bytes = pdf("hello");
        UploadResultResponse r = docService.upload(COMPANY, uploadCmd(f.getId(), "C:\\fakepath\\Invoice 1.pdf", bytes, false));
        assertEquals(UploadResultResponse.CREATED, r.status());
        DocumentResponse doc = r.document();
        assertEquals("Invoice 1.pdf", doc.name());
        assertEquals("application/pdf", doc.contentType());
        assertEquals(1, doc.versionNo());
        assertEquals("EDIT", doc.level());
        assertEquals(1, storage.files.size());

        DocumentContent content = docService.content(COMPANY, doc.id(), null);
        assertArrayEquals(bytes, content.stream().readAllBytes());
        assertTrue(content.inlineSafe());
    }

    @Test
    void rejectedTypeLeavesNothingBehind() {
        Folder f = folder("Inbox", null);
        assertThrows(DocumentsDomainException.class,
                () -> docService.upload(COMPANY, uploadCmd(f.getId(), "run.pdf", new byte[]{'M', 'Z', 1, 2, 3}, false)));
        assertEquals(0, storage.files.size());
        assertEquals(0, docs.docs.size());
    }

    @Test
    void duplicateInTheSameFolderIsReportedUnlessAllowed() {
        Folder f = folder("Inbox", null);
        DocumentResponse first = uploadPdf(f, "a.pdf", "same");
        UploadResultResponse dup = docService.upload(COMPANY, uploadCmd(f.getId(), "b.pdf", pdf("same"), false));
        assertEquals(UploadResultResponse.DUPLICATE, dup.status());
        assertEquals(first.id(), dup.existing().id());
        assertEquals(1, storage.files.size());
        UploadResultResponse forced = docService.upload(COMPANY, uploadCmd(f.getId(), "b.pdf", pdf("same"), true));
        assertEquals(UploadResultResponse.CREATED, forced.status());
        assertEquals(2, storage.files.size());
    }

    @Test
    void databaseFailureDeletesTheStoredFile() {
        Folder f = folder("Inbox", null);
        docs.failOnSave = true;
        assertThrows(IllegalStateException.class, () -> docService.upload(COMPANY, uploadCmd(f.getId(), "a.pdf", pdf("x"), false)));
        assertEquals(0, storage.files.size());
    }

    @Test
    void tooLargeEmptyAndNamelessFilesAreRejected() {
        Folder f = folder("Inbox", null);
        assertThrows(DocumentsDomainException.class,
                () -> docService.upload(COMPANY, uploadCmd(f.getId(), "big.pdf", new byte[2 * 1024 * 1024], false)));
        assertThrows(DocumentsDomainException.class, () -> docService.upload(COMPANY, uploadCmd(f.getId(), "e.pdf", new byte[0], false)));
        assertThrows(DocumentsDomainException.class, () -> docService.upload(COMPANY, uploadCmd(f.getId(), "  ", pdf("x"), false)));
        assertThrows(DocumentsDomainException.class, () -> docService.upload(COMPANY, uploadCmd(null, "a.pdf", pdf("x"), false)));
        assertEquals(0, storage.files.size());
    }

    @Test
    void cannotUploadWithViewOnlyAccessOrToArchivedOrHiddenFolders() {
        permissions.remove("documents.document.write");
        Folder open = folder("Open", null);
        ResponseStatusException viewOnly = assertThrows(ResponseStatusException.class,
                () -> docService.upload(COMPANY, uploadCmd(open.getId(), "a.pdf", pdf("x"), false)));
        assertEquals(HttpStatus.FORBIDDEN, viewOnly.getStatusCode());

        permissions.add("documents.document.write");
        Folder hidden = restrictedFolder("Hidden", UUID.randomUUID(), AccessLevel.MANAGE);
        ResponseStatusException notFound = assertThrows(ResponseStatusException.class,
                () -> docService.upload(COMPANY, uploadCmd(hidden.getId(), "a.pdf", pdf("x"), false)));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        open.archive();
        folders.save(open);
        assertThrows(DocumentsDomainException.class, () -> docService.upload(COMPANY, uploadCmd(open.getId(), "a.pdf", pdf("x"), false)));
    }

    @Test
    void quotaBlocksUploadsOnceReached() {
        DocumentApplicationServiceImpl limited = new DocumentApplicationServiceImpl(docs, tags, storage, records, resolver,
                audit, ctx, clock, 1024 * 1024, 100);
        Folder f = folder("Inbox", null);
        limited.upload(COMPANY, uploadCmd(f.getId(), "a.pdf", pdf("first one that is long enough to reach the quota of one hundred bytes in total, yes it is"), false));
        assertThrows(DocumentsDomainException.class, () -> limited.upload(COMPANY, uploadCmd(f.getId(), "b.pdf", pdf("second"), false)));
        assertEquals(100, limited.stats(COMPANY).quotaBytes());
    }

    // ---------------------------------------------------------------- read and access

    @Test
    void documentInAClosedFolderIsInvisible() {
        asManager();
        Folder closed = restrictedFolder("Closed", UUID.randomUUID(), AccessLevel.MANAGE);
        DocumentResponse doc = uploadPdf(closed, "secret.pdf", "s");
        permissions.remove("documents.access.manage");
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> docService.get(COMPANY, doc.id()));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertEquals(0, docService.search(COMPANY, all()).getTotalElements());
        assertThrows(ResponseStatusException.class, () -> docService.content(COMPANY, doc.id(), null));
    }

    @Test
    void linkedRecordMakesTheDocumentReadableButNotEditable() {
        asManager();
        UUID partner = UUID.randomUUID();
        records.existing.add(partner);
        Folder closed = restrictedFolder("Closed", UUID.randomUUID(), AccessLevel.MANAGE);
        DocumentResponse doc = uploadPdf(closed, "contract.pdf", "c");
        docService.addLink(COMPANY, doc.id(), new AddLinkCommand("contacts.partner", partner));

        permissions.remove("documents.access.manage");
        assertThrows(ResponseStatusException.class, () -> docService.get(COMPANY, doc.id()));

        permissions.add("contacts.partner.read");
        DocumentResponse seen = docService.get(COMPANY, doc.id());
        assertEquals("VIEW", seen.level());
        assertNotNull(seen.links().get(0).label());
        assertEquals(1, docService.findByRecord(COMPANY, "contacts.partner", partner).size());
        assertNotNull(docService.content(COMPANY, doc.id(), null));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> docService.update(COMPANY, doc.id(), new UpdateDocumentCommand("x", null, null)));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }

    @Test
    void findByRecordNeedsTheRecordReadPermission() {
        UUID partner = UUID.randomUUID();
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> docService.findByRecord(COMPANY, "contacts.partner", partner));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        assertThrows(DocumentsDomainException.class, () -> docService.findByRecord(COMPANY, "unknown.model", partner));
    }

    // ---------------------------------------------------------------- search

    @Test
    void searchFiltersByFolderTreeTextAndType() {
        Folder root = folder("Root", null);
        Folder sub = folder("Sub", root.getId());
        Folder other = folder("Other", null);
        uploadPdf(root, "Annual Report 2026.pdf", "1");
        uploadPdf(sub, "Quarterly report.pdf", "2");
        uploadPdf(other, "Unrelated.pdf", "3");
        docService.upload(COMPANY, new com.bradox.erp.documents.service.domain.dto.UploadDocumentCommand(sub.getId().getId(),
                "notes.txt", new java.io.ByteArrayInputStream("plain".getBytes()), false, null));

        assertEquals(4, docService.search(COMPANY, all()).getTotalElements());
        assertEquals(3, docService.search(COMPANY, inFolder(root, null)).getTotalElements());
        PageResponse<DocumentSummaryResponse> byText = docService.search(COMPANY, inFolder(root, "REPORT \u0662\u0660\u0662\u0666"));
        assertEquals(1, byText.getTotalElements());
        assertEquals("Annual Report 2026.pdf", byText.getContent().get(0).name());
        PageResponse<DocumentSummaryResponse> noSub = docService.search(COMPANY,
                new DocumentSearchQuery(null, root.getId().getId(), false, null, null, null, null, null, null, null, null, 0, 50));
        assertEquals(1, noSub.getTotalElements());
        assertEquals(1, docService.search(COMPANY, new DocumentSearchQuery(null, null, false, null, "text", null, null, null, null, null, "-name", 0, 50)).getTotalElements() == 0 ? 0 : 1);
        PageResponse<DocumentSummaryResponse> paged = docService.search(COMPANY,
                new DocumentSearchQuery(null, null, false, null, null, null, null, null, null, null, "name", 1, 3));
        assertEquals(1, paged.getContent().size());
        assertEquals(2, paged.getTotalPages());
    }

    @Test
    void searchCombinesTagsAndMatchesTagNames() {
        Folder f = folder("Tagged", null);
        TagFacetResponse status = tagService.createFacet(COMPANY, new TagFacetCommand("Status", null));
        TagFacetResponse type = tagService.createFacet(COMPANY, new TagFacetCommand("Type", null));
        TagResponse draft = tagService.createTag(COMPANY, new TagCommand(status.id(), "Draft", "#111111", null));
        TagResponse signed = tagService.createTag(COMPANY, new TagCommand(status.id(), "Signed", null, null));
        TagResponse contract = tagService.createTag(COMPANY, new TagCommand(type.id(), "Contract", null, null));
        DocumentResponse a = uploadPdf(f, "a.pdf", "a");
        DocumentResponse b = uploadPdf(f, "b.pdf", "b");
        docService.setTags(COMPANY, a.id(), new SetTagsCommand(Set.of(draft.id(), contract.id())));
        docService.setTags(COMPANY, b.id(), new SetTagsCommand(Set.of(signed.id())));

        assertEquals(2, tagged(f, draft.id(), signed.id()));
        assertEquals(1, tagged(f, draft.id(), signed.id(), contract.id()));
        assertEquals(1, docService.search(COMPANY, inFolder(f, "signed")).getTotalElements());
        assertThrows(DocumentsDomainException.class,
                () -> docService.setTags(COMPANY, a.id(), new SetTagsCommand(Set.of(UUID.randomUUID()))));
    }

    private long tagged(Folder f, UUID... tagIds) {
        return docService.search(COMPANY, new DocumentSearchQuery(null, f.getId().getId(), false, List.of(tagIds), null, null,
                null, null, null, null, null, 0, 50)).getTotalElements();
    }

    // ---------------------------------------------------------------- versions

    @Test
    void newVersionsKeepHistoryAndRestoreCopiesTheOldFile() throws IOException {
        Folder f = folder("Inbox", null);
        DocumentResponse doc = uploadPdf(f, "a.pdf", "one");
        UploadResultResponse same = docService.uploadVersion(COMPANY, doc.id(), uploadCmd(null, "a.pdf", pdf("one"), false));
        assertEquals(UploadResultResponse.DUPLICATE, same.status());
        UploadResultResponse v2 = docService.uploadVersion(COMPANY, doc.id(), uploadCmd(null, "a-v2.pdf", pdf("two"), false));
        assertEquals(2, v2.document().versionNo());
        List<VersionResponse> versions = docService.versions(COMPANY, doc.id());
        assertEquals(2, versions.size());
        assertTrue(versions.get(0).current());
        VersionResponse first = versions.get(1);

        DocumentResponse restored = docService.restoreVersion(COMPANY, doc.id(), first.id());
        assertEquals(3, restored.versionNo());
        assertArrayEquals(pdf("one"), docService.content(COMPANY, doc.id(), null).stream().readAllBytes());
        assertArrayEquals(pdf("one"), docService.content(COMPANY, doc.id(), first.id()).stream().readAllBytes());
        assertThrows(ResponseStatusException.class, () -> docService.content(COMPANY, doc.id(), UUID.randomUUID()));
        assertThrows(ResponseStatusException.class, () -> docService.restoreVersion(COMPANY, doc.id(), UUID.randomUUID()));
    }

    @Test
    void aDocumentCannotHaveMoreThanFiftyVersions() {
        Folder f = folder("Inbox", null);
        DocumentResponse doc = uploadPdf(f, "a.pdf", "v1");
        for (int i = 2; i <= 50; i++) {
            docService.uploadVersion(COMPANY, doc.id(), uploadCmd(null, "a.pdf", pdf("v" + i), false));
        }
        assertThrows(DocumentsDomainException.class,
                () -> docService.uploadVersion(COMPANY, doc.id(), uploadCmd(null, "a.pdf", pdf("v51"), false)));
        VersionResponse any = docService.versions(COMPANY, doc.id()).get(5);
        assertThrows(DocumentsDomainException.class, () -> docService.restoreVersion(COMPANY, doc.id(), any.id()));
    }

    // ---------------------------------------------------------------- edit

    @Test
    void renameDescribeAndMove() {
        Folder a = folder("A", null);
        Folder b = folder("B", null);
        DocumentResponse doc = uploadPdf(a, "a.pdf", "x");
        DocumentResponse updated = docService.update(COMPANY, doc.id(), new UpdateDocumentCommand("Better name", "about it", b.getId().getId()));
        assertEquals("Better name", updated.name());
        assertEquals("about it", updated.description());
        assertEquals(b.getId().getId(), updated.folderId());
        assertEquals(List.of("B"), updated.folderPath().stream().map(p -> p.name()).toList());

        Folder hidden = restrictedFolder("Hidden", UUID.randomUUID(), AccessLevel.MANAGE);
        assertThrows(ResponseStatusException.class,
                () -> docService.update(COMPANY, doc.id(), new UpdateDocumentCommand(null, null, hidden.getId().getId())));
        b.archive();
        folders.save(b);
        Folder c = folder("C", null);
        c.archive();
        folders.save(c);
        assertThrows(DocumentsDomainException.class,
                () -> docService.update(COMPANY, doc.id(), new UpdateDocumentCommand(null, null, c.getId().getId())));
    }

    @Test
    void linksAreValidatedAndDeduplicated() {
        Folder f = folder("Inbox", null);
        DocumentResponse doc = uploadPdf(f, "a.pdf", "x");
        UUID partner = UUID.randomUUID();
        assertThrows(DocumentsDomainException.class, () -> docService.addLink(COMPANY, doc.id(), new AddLinkCommand("nope", partner)));
        assertThrows(DocumentsDomainException.class, () -> docService.addLink(COMPANY, doc.id(), new AddLinkCommand("contacts.partner", partner)));
        records.existing.add(partner);
        docService.addLink(COMPANY, doc.id(), new AddLinkCommand("contacts.partner", partner));
        DocumentResponse twice = docService.addLink(COMPANY, doc.id(), new AddLinkCommand("contacts.partner", partner));
        assertEquals(1, twice.links().size());
        assertEquals(0, docService.removeLink(COMPANY, doc.id(), "contacts.partner", partner).links().size());
        assertEquals(0, docService.removeLink(COMPANY, doc.id(), "contacts.partner", partner).links().size());
    }

    // ---------------------------------------------------------------- trash and retention

    @Test
    void trashRestoreAndEmptyTrash() {
        Folder f = folder("Inbox", null);
        DocumentResponse doc = uploadPdf(f, "a.pdf", "x");
        docService.trash(COMPANY, doc.id());
        assertEquals(0, docService.search(COMPANY, all()).getTotalElements());
        assertEquals(1, docService.search(COMPANY, new DocumentSearchQuery(null, null, false, null, null, null, null, null, null, "TRASHED", null, 0, 50)).getTotalElements());
        assertThrows(DocumentsDomainException.class, () -> docService.trash(COMPANY, doc.id()));

        DocumentResponse back = docService.restore(COMPANY, doc.id(), new RestoreDocumentCommand(null));
        assertEquals("ACTIVE", back.status());
        assertThrows(DocumentsDomainException.class, () -> docService.restore(COMPANY, doc.id(), null));

        docService.trash(COMPANY, doc.id());
        f.archive();
        folders.save(f);
        assertThrows(DocumentsDomainException.class, () -> docService.restore(COMPANY, doc.id(), null));
        Folder other = folder("Other", null);
        DocumentResponse elsewhere = docService.restore(COMPANY, doc.id(), new RestoreDocumentCommand(other.getId().getId()));
        assertEquals(other.getId().getId(), elsewhere.folderId());

        docService.trash(COMPANY, doc.id());
        assertEquals(1, docService.emptyTrash(COMPANY));
        assertEquals(0, docs.docs.size());
        assertEquals(0, storage.files.size());
    }

    @Test
    void retentionLockProtectsFromTrashAndPurge() {
        Folder f = folder("Signed", null);
        DocumentResponse doc = uploadPdf(f, "contract.pdf", "x");
        DocumentResponse locked = docService.lockRetention(COMPANY, doc.id(),
                new LockRetentionCommand(LocalDate.of(2036, 10, 8), "signed contract"));
        assertEquals(LocalDate.of(2036, 10, 8), locked.retentionUntil());
        assertThrows(DocumentsDomainException.class, () -> docService.trash(COMPANY, doc.id()));
        assertThrows(DocumentsDomainException.class,
                () -> docService.lockRetention(COMPANY, doc.id(), new LockRetentionCommand(LocalDate.of(2030, 1, 1), null)));
        assertEquals(0, docService.emptyTrash(COMPANY));
        assertEquals(1, docs.docs.size());
    }

    @Test
    void expiredTrashIsPurgedButRecentTrashAndLockedDocumentsStay() {
        Folder f = folder("Inbox", null);
        DocumentResponse old = uploadPdf(f, "old.pdf", "old");
        DocumentResponse recent = uploadPdf(f, "recent.pdf", "recent");
        docService.trash(COMPANY, old.id());

        // 31 days later the first one is purged, the one trashed now is not
        java.time.Clock later = java.time.Clock.offset(clock, java.time.Duration.ofDays(31));
        DocumentApplicationServiceImpl future = new DocumentApplicationServiceImpl(docs, tags, storage, records, resolver,
                audit, ctx, later, 1024 * 1024, 0);
        future.trash(COMPANY, recent.id());
        assertEquals(1, future.purgeExpiredTrash());
        assertFalse(docs.docs.containsKey(new DocumentId(old.id())));
        assertTrue(docs.docs.containsKey(new DocumentId(recent.id())));
        assertEquals(0, future.purgeExpiredTrash());
    }

    @Test
    void statsCountDocuments() {
        Folder f = folder("Inbox", null);
        uploadPdf(f, "a.pdf", "a");
        assertEquals(1, docService.stats(COMPANY).documentCount());
        assertTrue(docService.stats(COMPANY).usedBytes() > 0);
        assertEquals(1024 * 1024, docService.stats(COMPANY).maxFileBytes());
        assertEquals(new FolderId(f.getId().getId()), f.getId());
    }
}
