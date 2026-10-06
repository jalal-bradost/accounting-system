package com.bradox.erp.accounting.service.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Undirected graph of trade records (documents, payments, allocations) used to derive
 * reconciliation groups. Each node carries the journal entries whose receivable/payable lines it
 * owns; connected components become candidate reconciliation groups.
 */
public final class TradeReconciliationGraph {

    private final Map<UUID, Set<UUID>> entriesByNode = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> adjacency = new HashMap<>();
    private final List<Set<UUID>> standalone = new ArrayList<>();

    public void addNode(UUID nodeId, Collection<UUID> journalEntryIds) {
        Set<UUID> entries = entriesByNode.computeIfAbsent(nodeId, k -> new LinkedHashSet<>());
        if (journalEntryIds != null) {
            journalEntryIds.stream().filter(Objects::nonNull).forEach(entries::add);
        }
        adjacency.computeIfAbsent(nodeId, k -> new LinkedHashSet<>());
    }

    public void addEdge(UUID a, UUID b) {
        addNode(a, null);
        addNode(b, null);
        adjacency.get(a).add(b);
        adjacency.get(b).add(a);
    }

    /** A self-contained group (e.g. an entry and its reversal) not linked to any node. */
    public void addStandalone(Collection<UUID> journalEntryIds) {
        Set<UUID> entries = new LinkedHashSet<>();
        journalEntryIds.stream().filter(Objects::nonNull).forEach(entries::add);
        if (!entries.isEmpty()) {
            standalone.add(entries);
        }
    }

    /** Journal entry ids per connected component (plus standalone groups). */
    public List<Set<UUID>> components() {
        List<Set<UUID>> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        for (UUID start : entriesByNode.keySet()) {
            if (!visited.add(start)) continue;
            Set<UUID> entries = new LinkedHashSet<>();
            Deque<UUID> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                UUID node = queue.poll();
                entries.addAll(entriesByNode.getOrDefault(node, Set.of()));
                for (UUID next : adjacency.getOrDefault(node, Set.of())) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            if (!entries.isEmpty()) {
                result.add(entries);
            }
        }
        result.addAll(standalone);
        return result;
    }
}
