package com.bradox.erp.documents.domain.core.valueobject;

/**
 * Per-folder access level, ordered. VIEW reads and downloads, EDIT uploads, renames, tags, links
 * and trashes, MANAGE changes folder structure and access.
 */
public enum AccessLevel {
    NONE(0),
    VIEW(1),
    EDIT(2),
    MANAGE(3);

    private final int rank;

    AccessLevel(int rank) {
        this.rank = rank;
    }

    public boolean atLeast(AccessLevel other) {
        return rank >= other.rank;
    }

    public static AccessLevel max(AccessLevel a, AccessLevel b) {
        return a.rank >= b.rank ? a : b;
    }
}
