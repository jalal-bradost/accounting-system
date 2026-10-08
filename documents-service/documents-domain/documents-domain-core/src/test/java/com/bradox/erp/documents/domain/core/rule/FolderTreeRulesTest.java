package com.bradox.erp.documents.domain.core.rule;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;
import com.bradox.erp.domain.valueobject.CompanyId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FolderTreeRulesTest {

    static final CompanyId COMPANY = new CompanyId(UUID.randomUUID());

    static Folder folder(FolderId parent, String name) {
        return Folder.create(new FolderId(UUID.randomUUID()), COMPANY, parent, name, 0, null, null, "tester", Instant.now());
    }

    @Test
    void validateNameRejectsBlankLongAndInvalidCharacters() {
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.validateName(" "));
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.validateName(null));
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.validateName("a".repeat(256)));
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.validateName("a/b"));
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.validateName("a\\b"));
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.validateName("a\u0000b"));
        FolderTreeRules.validateName("المالية");
    }

    @Test
    void siblingNamesAreUniqueIgnoringCaseAndArabicVariants() {
        Folder finance = folder(null, "Finance");
        FolderTree tree = FolderTree.of(List.of(finance));
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.assertCanCreateUnder(tree, null, " finance "));
        FolderTreeRules.assertCanCreateUnder(tree, finance.getId(), "Finance");
    }

    @Test
    void createUnderUnknownParentFails() {
        FolderTree tree = FolderTree.of(List.of());
        assertThrows(DocumentsDomainException.class,
                () -> FolderTreeRules.assertCanCreateUnder(tree, new FolderId(UUID.randomUUID()), "x"));
    }

    @Test
    void maximumDepthIsEight() {
        List<Folder> chain = new ArrayList<>();
        Folder parent = null;
        for (int i = 0; i < FolderTreeRules.MAX_DEPTH; i++) {
            Folder f = folder(parent == null ? null : parent.getId(), "level" + i);
            chain.add(f);
            parent = f;
        }
        FolderTree tree = FolderTree.of(chain);
        assertEquals(8, tree.depthOf(parent.getId()));
        Folder deepest = parent;
        assertThrows(DocumentsDomainException.class,
                () -> FolderTreeRules.assertCanCreateUnder(tree, deepest.getId(), "too deep"));
        FolderTreeRules.assertCanCreateUnder(tree, chain.get(6).getId(), "level8 is ok under level7");
    }

    @Test
    void moveRejectsCyclesAndDuplicatesAndExcessDepth() {
        Folder a = folder(null, "A");
        Folder b = folder(a.getId(), "B");
        Folder c = folder(b.getId(), "C");
        Folder otherRoot = folder(null, "B");
        FolderTree tree = FolderTree.of(List.of(a, b, c, otherRoot));

        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.assertCanMove(tree, a, c.getId()));
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.assertCanMove(tree, a, a.getId()));
        // moving B (under A) to root clashes with the existing root named B
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.assertCanMove(tree, b, null));
        // moving C to root is fine
        FolderTreeRules.assertCanMove(tree, c, null);
        assertThrows(DocumentsDomainException.class,
                () -> FolderTreeRules.assertCanMove(tree, c, new FolderId(UUID.randomUUID())));
    }

    @Test
    void moveRejectsSubtreeThatWouldExceedDepth() {
        List<Folder> all = new ArrayList<>();
        Folder p = null;
        for (int i = 0; i < 6; i++) {
            Folder f = folder(p == null ? null : p.getId(), "n" + i);
            all.add(f);
            p = f;
        }
        Folder rootA = all.get(0);
        Folder subRoot = folder(null, "sub");
        Folder subChild = folder(subRoot.getId(), "subchild");
        Folder subGrand = folder(subChild.getId(), "subgrand");
        all.add(subRoot);
        all.add(subChild);
        all.add(subGrand);
        FolderTree tree = FolderTree.of(all);
        Folder deepest = all.get(5);
        // depth of deepest is 6, subtree height is 3 -> 9 > 8
        assertThrows(DocumentsDomainException.class, () -> FolderTreeRules.assertCanMove(tree, subRoot, deepest.getId()));
        FolderTreeRules.assertCanMove(tree, subRoot, rootA.getId());
    }

    @Test
    void treeHelpers() {
        Folder a = folder(null, "A");
        Folder b = folder(a.getId(), "B");
        Folder c = folder(b.getId(), "C");
        FolderTree tree = FolderTree.of(List.of(a, b, c));
        assertEquals(3, tree.heightOf(a.getId()));
        assertEquals(1, tree.heightOf(c.getId()));
        assertEquals(3, tree.subtreeIds(a.getId()).size());
        assertEquals(List.of(b, a), tree.ancestorsOf(c.getId()));
        assertTrue(tree.contains(b.getId()));
        assertFalse(tree.contains(new FolderId(UUID.randomUUID())));
        assertEquals(1, tree.childrenOf(a.getId()).size());
        assertEquals(0, tree.depthOf(new FolderId(UUID.randomUUID())) - 1 + 0 * tree.all().size());
    }
}
