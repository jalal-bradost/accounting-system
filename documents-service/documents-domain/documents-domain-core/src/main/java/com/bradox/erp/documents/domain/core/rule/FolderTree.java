package com.bradox.erp.documents.domain.core.rule;

import com.bradox.erp.documents.domain.core.entity.Folder;
import com.bradox.erp.documents.domain.core.valueobject.FolderId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** In-memory view of all folders of a company, used for cycle, depth and ancestor checks. */
public final class FolderTree {

    private final Map<FolderId, Folder> byId = new HashMap<>();
    private final Map<FolderId, List<Folder>> children = new HashMap<>();

    private FolderTree(Collection<Folder> folders) {
        for (Folder f : folders) {
            byId.put(f.getId(), f);
        }
        for (Folder f : folders) {
            if (f.getParentId() != null) {
                children.computeIfAbsent(f.getParentId(), k -> new ArrayList<>()).add(f);
            }
        }
    }

    public static FolderTree of(Collection<Folder> folders) {
        return new FolderTree(folders);
    }

    public Folder get(FolderId id) {
        return byId.get(id);
    }

    public boolean contains(FolderId id) {
        return byId.containsKey(id);
    }

    public Collection<Folder> all() {
        return byId.values();
    }

    public List<Folder> childrenOf(FolderId id) {
        return children.getOrDefault(id, List.of());
    }

    /** Depth of a folder: a root folder has depth 1. */
    public int depthOf(FolderId id) {
        int depth = 0;
        Set<FolderId> seen = new HashSet<>();
        FolderId cursor = id;
        while (cursor != null && seen.add(cursor)) {
            depth++;
            Folder f = byId.get(cursor);
            cursor = f == null ? null : f.getParentId();
        }
        return depth;
    }

    /** Height of the subtree below a folder, counting the folder itself (a leaf has height 1). */
    public int heightOf(FolderId id) {
        int best = 0;
        Deque<Object[]> stack = new ArrayDeque<>();
        stack.push(new Object[]{id, 1});
        Set<FolderId> seen = new HashSet<>();
        while (!stack.isEmpty()) {
            Object[] top = stack.pop();
            FolderId cur = (FolderId) top[0];
            int h = (Integer) top[1];
            if (!seen.add(cur)) {
                continue;
            }
            best = Math.max(best, h);
            for (Folder c : childrenOf(cur)) {
                stack.push(new Object[]{c.getId(), h + 1});
            }
        }
        return best;
    }

    /** The folder and all its descendants. */
    public Set<FolderId> subtreeIds(FolderId id) {
        Set<FolderId> out = new HashSet<>();
        Deque<FolderId> stack = new ArrayDeque<>();
        stack.push(id);
        while (!stack.isEmpty()) {
            FolderId cur = stack.pop();
            if (!out.add(cur)) {
                continue;
            }
            for (Folder c : childrenOf(cur)) {
                stack.push(c.getId());
            }
        }
        return out;
    }

    /** Ancestors from the nearest parent up to the root (excluding the folder itself). */
    public List<Folder> ancestorsOf(FolderId id) {
        List<Folder> out = new ArrayList<>();
        Set<FolderId> seen = new HashSet<>();
        Folder f = byId.get(id);
        while (f != null && f.getParentId() != null && seen.add(f.getId())) {
            Folder parent = byId.get(f.getParentId());
            if (parent == null) {
                break;
            }
            out.add(parent);
            f = parent;
        }
        return out;
    }
}
