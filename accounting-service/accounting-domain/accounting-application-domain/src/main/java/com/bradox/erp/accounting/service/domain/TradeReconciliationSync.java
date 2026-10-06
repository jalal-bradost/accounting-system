package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort.ItemSnapshot;
import com.bradox.erp.domain.core.ValueObject.AccountType;
import com.bradox.erp.domain.core.ValueObject.JournalItemId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Derives {@code journal_items.reconciliation_id} on receivable/payable lines from payment
 * allocations. For every connected component (one partner, one trade account), the lines are
 * tagged with one deterministic id when they net to exactly zero in company currency; otherwise
 * the tag is cleared. Never hand-edit trade tags: this is their only writer.
 */
@Component
public class TradeReconciliationSync {

    private final JournalItemReconciliationPort journalItemReconciliationPort;

    public TradeReconciliationSync(JournalItemReconciliationPort journalItemReconciliationPort) {
        this.journalItemReconciliationPort = journalItemReconciliationPort;
    }

    @Transactional
    public void apply(UUID partnerId, AccountType tradeType, List<Set<UUID>> components) {
        if (partnerId == null || components == null || components.isEmpty()) return;
        Set<UUID> allEntries = new LinkedHashSet<>();
        components.forEach(allEntries::addAll);
        Map<UUID, List<ItemSnapshot>> itemsByEntry = new HashMap<>();
        for (List<UUID> chunk : chunks(allEntries, 500)) {
            for (ItemSnapshot item : journalItemReconciliationPort.findItemsByEntryIds(chunk)) {
                if (item.accountType() != tradeType
                        || !partnerId.equals(item.partnerId())
                        || !"POSTED".equals(item.entryStatus())) {
                    continue;
                }
                itemsByEntry.computeIfAbsent(item.journalEntryId(), k -> new ArrayList<>()).add(item);
            }
        }

        Map<UUID, List<UUID>> toTag = new LinkedHashMap<>();
        List<UUID> toClear = new ArrayList<>();
        for (Set<UUID> component : components) {
            Map<UUID, List<ItemSnapshot>> byAccount = new LinkedHashMap<>();
            for (UUID entryId : component) {
                for (ItemSnapshot item : itemsByEntry.getOrDefault(entryId, List.of())) {
                    byAccount.computeIfAbsent(item.accountId(), k -> new ArrayList<>()).add(item);
                }
            }
            for (List<ItemSnapshot> items : byAccount.values()) {
                UUID target = null;
                if (items.size() >= 2) {
                    BigDecimal net = items.stream().map(ItemSnapshot::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (net.signum() == 0) {
                        target = deterministicId(items);
                    }
                }
                for (ItemSnapshot item : items) {
                    if (Objects.equals(item.reconciliationId(), target)) continue;
                    if (target == null) {
                        toClear.add(item.itemId());
                    } else {
                        toTag.computeIfAbsent(target, k -> new ArrayList<>()).add(item.itemId());
                    }
                }
            }
        }
        if (!toClear.isEmpty()) {
            journalItemReconciliationPort.clearReconciliation(toClear.stream().map(JournalItemId::new).toList());
        }
        toTag.forEach((tag, ids) -> journalItemReconciliationPort.setReconciliation(
                ids.stream().map(JournalItemId::new).toList(), tag));
    }

    private static UUID deterministicId(List<ItemSnapshot> items) {
        String key = items.stream()
                .map(i -> i.itemId().toString())
                .sorted()
                .collect(Collectors.joining(",", "trade:", ""));
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static List<List<UUID>> chunks(Collection<UUID> ids, int size) {
        List<List<UUID>> out = new ArrayList<>();
        List<UUID> current = new ArrayList<>(size);
        for (UUID id : ids) {
            current.add(id);
            if (current.size() == size) {
                out.add(current);
                current = new ArrayList<>(size);
            }
        }
        if (!current.isEmpty()) out.add(current);
        return out;
    }
}
