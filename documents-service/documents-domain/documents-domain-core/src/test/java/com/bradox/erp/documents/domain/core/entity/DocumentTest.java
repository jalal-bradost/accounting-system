package com.bradox.erp.documents.domain.core.entity;

import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.DocumentId;
import com.bradox.erp.documents.domain.core.valueobject.DocumentSource;
import com.bradox.erp.documents.domain.core.valueobject.DocumentStatus;
import com.bradox.erp.documents.domain.core.valueobject.DocumentVersionId;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.documents.domain.core.valueobject.TagFacetId;
import com.bradox.erp.documents.domain.core.valueobject.TagId;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTest {

    private static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());
    private final Instant now = Instant.parse("2026-10-08T10:00:00Z");
    private final LocalDate today = LocalDate.of(2026, 10, 8);

    private Document newDoc() {
        return Document.create(new DocumentId(UUID.randomUUID()), COMPANY, new FolderId(UUID.randomUUID()),
                "Invoice 2026", "first", DocumentSource.UPLOAD, UUID.randomUUID(), "tester", now);
    }

    private DocumentVersion version(Document d, int no) {
        return new DocumentVersion(new DocumentVersionId(UUID.randomUUID()), d.getId(), no, UUID.randomUUID().toString(),
                "invoice.pdf", "application/pdf", 1234, "abc", "tester", now, "c");
    }

    @Test
    void createValidatesAndNormalizes() {
        Document d = newDoc();
        assertEquals("invoice 2026", d.getNameNormalized());
        assertEquals(DocumentStatus.ACTIVE, d.getStatus());
        assertEquals(0, d.getVersionNo());
        assertEquals(1, d.nextVersionNo());
        assertNull(d.takePendingVersion());
        assertThrows(DocumentsDomainException.class, () -> Document.create(new DocumentId(UUID.randomUUID()), COMPANY, null,
                "x", null, null, null, "t", now));
        assertThrows(DocumentsDomainException.class, () -> Document.create(new DocumentId(UUID.randomUUID()), COMPANY,
                new FolderId(UUID.randomUUID()), "", null, null, null, "t", now));
        assertThrows(DocumentsDomainException.class, () -> Document.create(new DocumentId(UUID.randomUUID()), COMPANY,
                new FolderId(UUID.randomUUID()), "x", "d".repeat(Document.MAX_DESCRIPTION_LENGTH + 1), null, null, "t", now));
        Document noSource = Document.create(new DocumentId(UUID.randomUUID()), COMPANY, new FolderId(UUID.randomUUID()),
                "x", "  ", null, null, "t", now);
        assertEquals(DocumentSource.UPLOAD, noSource.getSource());
        assertNull(noSource.getDescription());
    }

    @Test
    void addVersionUpdatesCurrentDetailsAndEnforcesNumbering() {
        Document d = newDoc();
        DocumentVersion v1 = version(d, 1);
        d.addVersion(v1, "u", now);
        assertEquals(1, d.getVersionNo());
        assertEquals("invoice.pdf", d.getFileName());
        assertEquals(1234, d.getSizeBytes());
        assertEquals(v1.getId(), d.getCurrentVersionId());
        assertEquals(v1, d.takePendingVersion());
        assertNull(d.takePendingVersion());
        assertThrows(DocumentsDomainException.class, () -> d.addVersion(version(d, 3), "u", now));
        d.addVersion(version(d, 2), "u", now);
        assertEquals(2, d.getVersionNo());
    }

    @Test
    void renameMoveAndDescribe() {
        Document d = newDoc();
        d.rename("  New name ", "u", now);
        assertEquals("New name", d.getName());
        assertEquals("new name", d.getNameNormalized());
        FolderId target = new FolderId(UUID.randomUUID());
        d.moveTo(target, "u", now);
        assertEquals(target, d.getFolderId());
        assertThrows(DocumentsDomainException.class, () -> d.moveTo(null, "u", now));
        d.describe("hello", "u", now);
        assertEquals("hello", d.getDescription());
        assertEquals("u", d.getUpdatedBy());
    }

    @Test
    void trashAndRestoreAndGuards() {
        Document d = newDoc();
        d.trash("u", now, today);
        assertEquals(DocumentStatus.TRASHED, d.getStatus());
        assertEquals("u", d.getTrashedBy());
        assertThrows(DocumentsDomainException.class, () -> d.trash("u", now, today));
        assertThrows(DocumentsDomainException.class, () -> d.rename("x", "u", now));
        assertThrows(DocumentsDomainException.class, () -> d.addVersion(version(d, 1), "u", now));
        assertThrows(DocumentsDomainException.class, () -> d.addLink(new RecordLink("hr.employee", UUID.randomUUID(), now, "u"), "u", now));
        assertTrue(d.canBePurged(today));
        d.restoreFromTrash("u", now);
        assertEquals(DocumentStatus.ACTIVE, d.getStatus());
        assertNull(d.getTrashedAt());
        assertThrows(DocumentsDomainException.class, () -> d.restoreFromTrash("u", now));
        assertFalse(d.canBePurged(today));
    }

    @Test
    void retentionLockBlocksTrashAndPurgeButNeverShortens() {
        Document d = newDoc();
        d.lockUntil(today.plusYears(10), "signed contract", "u", now);
        assertTrue(d.isRetentionLocked(today));
        assertThrows(DocumentsDomainException.class, () -> d.trash("u", now, today));
        assertThrows(DocumentsDomainException.class, () -> d.lockUntil(today.plusYears(1), "x", "u", now));
        assertThrows(DocumentsDomainException.class, () -> d.lockUntil(null, "x", "u", now));
        d.lockUntil(today.plusYears(12), "extended", "u", now);
        assertEquals(today.plusYears(12), d.getRetentionUntil());
        assertEquals("extended", d.getRetentionReason());
        // once the date has passed the lock is gone
        assertFalse(d.isRetentionLocked(today.plusYears(13)));
        d.trash("u", now, today.plusYears(13));
        assertTrue(d.canBePurged(today.plusYears(13)));
    }

    @Test
    void tagsAndLinks() {
        Document d = newDoc();
        TagId t1 = new TagId(UUID.randomUUID());
        d.replaceTags(Set.of(t1), "u", now);
        assertEquals(Set.of(t1), d.getTagIds());
        UUID rec = UUID.randomUUID();
        assertTrue(d.addLink(new RecordLink("accounting.customer-invoice", rec, now, "u"), "u", now));
        assertFalse(d.addLink(new RecordLink("accounting.customer-invoice", rec, now, "u"), "u", now));
        assertEquals(1, d.getLinks().size());
        assertFalse(d.removeLink("accounting.customer-invoice", UUID.randomUUID(), "u", now));
        assertTrue(d.removeLink("accounting.customer-invoice", rec, "u", now));
        assertEquals(0, d.getLinks().size());
    }

    @Test
    void restoreRebuildsState() {
        Document d = newDoc();
        d.addVersion(version(d, 1), "u", now);
        Document copy = Document.restore(d.getId(), COMPANY, d.getFolderId(), d.getName(), d.getNameNormalized(),
                d.getDescription(), d.getStatus(), d.getSource(), d.getOwnerUserId(), d.getCurrentVersionId(),
                d.getVersionNo(), d.getFileName(), d.getContentType(), d.getSizeBytes(), d.getSha256(),
                null, null, null, null, now, "c", now, "u", null, null);
        assertEquals(1, copy.getVersionNo());
        assertEquals(0, copy.getTagIds().size());
        assertNotNull(copy.getCurrentVersionId());
    }

    @Test
    void recordLinkValidation() {
        assertThrows(DocumentsDomainException.class, () -> new RecordLink(" ", UUID.randomUUID(), now, "u"));
        assertThrows(DocumentsDomainException.class, () -> new RecordLink("m", null, now, "u"));
        UUID id = UUID.randomUUID();
        assertEquals(new RecordLink("m", id, now, "a"), new RecordLink("m", id, now, "b"));
        assertEquals(new RecordLink("m", id, now, "a").hashCode(), new RecordLink("m", id, now, "b").hashCode());
    }

    @Test
    void tagsAndFacets() {
        TagFacet facet = TagFacet.create(new TagFacetId(UUID.randomUUID()), COMPANY, " Status ", 1);
        assertEquals("Status", facet.getName());
        facet.update("Type", 2);
        assertEquals(2, facet.getSequence());
        Tag tag = Tag.create(new TagId(UUID.randomUUID()), COMPANY, facet.getId(), "Draft", null, 0);
        assertEquals("#64748b", tag.getColor());
        tag.update("Final", "#16a34a", 3);
        assertEquals("#16a34a", tag.getColor());
        tag.update("Final", " ", 3);
        assertEquals("#16a34a", tag.getColor());
        assertThrows(DocumentsDomainException.class, () -> Tag.create(new TagId(UUID.randomUUID()), COMPANY, facet.getId(), "", null, 0));
    }

    @Test
    void folderBehaviour() {
        Folder f = Folder.create(new FolderId(UUID.randomUUID()), COMPANY, null, " Finance ", 2, " hr.employee ", "FINANCE", "u", now);
        assertEquals("Finance", f.getName());
        assertEquals("hr.employee", f.getLinkedModel());
        assertTrue(f.isInheritAccess());
        f.rename("Accounts");
        assertEquals("accounts", f.getNameNormalized());
        FolderId parent = new FolderId(UUID.randomUUID());
        f.moveTo(parent);
        assertEquals(parent, f.getParentId());
        f.archive();
        assertTrue(f.isArchived());
        f.unarchive();
        assertFalse(f.isArchived());
        f.changeSequence(9);
        f.changeLinkedModel(" ");
        assertNull(f.getLinkedModel());
        assertEquals(9, f.getSequence());
        assertThrows(DocumentsDomainException.class, () -> Folder.create(new FolderId(UUID.randomUUID()), null, null, "x", 0, null, null, "u", now));
    }
}
