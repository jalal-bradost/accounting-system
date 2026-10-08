package com.bradox.erp.documents.domain.core.rule;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.exception.DocumentsDomainException;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;

/** BR-DOC-01: unique sibling names, no cycles, maximum depth 8. */
public final class FolderTreeRules {

    public static final int MAX_DEPTH = 8;
    public static final int MAX_NAME_LENGTH = 255;

    private FolderTreeRules() {
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DocumentsDomainException("error.documents.nameRequired", null, "Name is required");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new DocumentsDomainException("error.documents.nameTooLong", new Object[]{MAX_NAME_LENGTH},
                    "Name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '/' || c == '\\' || Character.isISOControl(c)) {
                throw new DocumentsDomainException("error.documents.nameInvalidChars", null,
                        "Name must not contain slashes or control characters");
            }
        }
    }

    /** Checks creating a folder under {@code parentId} (null for a root folder). */
    public static void assertCanCreateUnder(FolderTree tree, FolderId parentId, String name) {
        validateName(name);
        if (parentId != null) {
            if (!tree.contains(parentId)) {
                throw new DocumentsDomainException("error.documents.parentNotFound", null, "Parent folder not found");
            }
            if (tree.depthOf(parentId) + 1 > MAX_DEPTH) {
                throw maxDepth();
            }
        }
        assertUniqueSibling(tree, parentId, name, null);
    }

    /** Checks moving {@code folder} under {@code newParentId} (null for root). */
    public static void assertCanMove(FolderTree tree, Folder folder, FolderId newParentId) {
        if (newParentId != null) {
            if (!tree.contains(newParentId)) {
                throw new DocumentsDomainException("error.documents.parentNotFound", null, "Parent folder not found");
            }
            if (tree.subtreeIds(folder.getId()).contains(newParentId)) {
                throw new DocumentsDomainException("error.documents.folderCycle", null,
                        "A folder cannot be moved into itself or one of its sub-folders");
            }
            if (tree.depthOf(newParentId) + tree.heightOf(folder.getId()) > MAX_DEPTH) {
                throw maxDepth();
            }
        } else if (tree.heightOf(folder.getId()) > MAX_DEPTH) {
            throw maxDepth();
        }
        assertUniqueSibling(tree, newParentId, folder.getName(), folder.getId());
    }

    public static void assertUniqueSibling(FolderTree tree, FolderId parentId, String name, FolderId ignore) {
        String normalized = NameNormalizer.normalize(name);
        for (Folder f : tree.all()) {
            if (ignore != null && f.getId().equals(ignore)) {
                continue;
            }
            boolean sameParent = parentId == null ? f.getParentId() == null : parentId.equals(f.getParentId());
            if (sameParent && f.getNameNormalized().equals(normalized)) {
                throw new DocumentsDomainException("error.documents.folderNameExists", new Object[]{name},
                        "A folder named '" + name + "' already exists here");
            }
        }
    }

    private static DocumentsDomainException maxDepth() {
        return new DocumentsDomainException("error.documents.folderTooDeep", new Object[]{MAX_DEPTH},
                "Folders can be nested at most " + MAX_DEPTH + " levels deep");
    }
}
