package com.bradox.erp.accounting.service.domain;

import com.bradox.erp.accounting.service.domain.ports.input.service.ReconciliationApplicationService;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort;
import com.bradox.erp.accounting.service.domain.ports.output.repository.JournalItemReconciliationPort.ItemSnapshot;
import com.bradox.erp.domain.core.ValueObject.JournalItemId;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.platform.web.CompanyContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manual reconciliation of non-trade lines. Receivable/payable lines are owned by payment
 * allocations (see {@link TradeReconciliationSync}) and cannot be tagged by hand.
 */
@Service
public class ReconciliationApplicationServiceImpl implements ReconciliationApplicationService {

    private final JournalItemReconciliationPort journalItemReconciliationPort;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    public ReconciliationApplicationServiceImpl(JournalItemReconciliationPort journalItemReconciliationPort,
                                                ObjectProvider<CompanyContext> companyContextProvider) {
        this.journalItemReconciliationPort = journalItemReconciliationPort;
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    @Transactional
    public void reconcile(ReconcileCommand command) {
        if (command.journalItemIds() == null || command.journalItemIds().isEmpty()) return;
        if (command.reconciliationId() == null) {
            throw new IllegalArgumentException("reconciliationId is required");
        }
        Set<UUID> uniqueIds = new LinkedHashSet<>(command.journalItemIds());
        if (uniqueIds.size() < 2) {
            throw new AccountingDomainException("error.accounting.reconcileMinTwoLines", null,
                    "At least two journal lines are required to reconcile");
        }
        List<ItemSnapshot> items = loadAll(uniqueIds);
        ItemSnapshot first = items.get(0);
        BigDecimal net = BigDecimal.ZERO;
        for (ItemSnapshot item : items) {
            rejectTrade(item);
            if (!"POSTED".equals(item.entryStatus())) {
                throw new AccountingDomainException("error.accounting.reconcileOnlyPosted", null,
                        "Only posted journal lines can be reconciled");
            }
            if (!Objects.equals(first.companyId(), item.companyId())
                    || !Objects.equals(first.accountId(), item.accountId())) {
                throw new AccountingDomainException("error.accounting.reconcileSameAccount", null,
                        "All lines must belong to the same account");
            }
            if (!Objects.equals(first.partnerId(), item.partnerId())) {
                throw new AccountingDomainException("error.accounting.reconcileSamePartner", null,
                        "All lines must belong to the same partner");
            }
            if (item.reconciliationId() != null && !item.reconciliationId().equals(command.reconciliationId())) {
                throw new AccountingDomainException("error.accounting.reconcileAlreadyReconciled", null,
                        "A line is already reconciled; unreconcile it first");
            }
            net = net.add(item.balance());
        }
        if (net.signum() != 0) {
            throw new AccountingDomainException("error.accounting.reconcileNotBalanced", null,
                    "Selected lines do not net to zero");
        }
        journalItemReconciliationPort.setReconciliation(
                uniqueIds.stream().map(JournalItemId::new).collect(Collectors.toList()),
                command.reconciliationId());
    }

    @Override
    @Transactional
    public void unreconcile(UnreconcileCommand command) {
        if (command.journalItemIds() == null || command.journalItemIds().isEmpty()) return;
        Set<UUID> uniqueIds = new LinkedHashSet<>(command.journalItemIds());
        List<ItemSnapshot> items = loadAll(uniqueIds);
        items.forEach(this::rejectTrade);
        Set<UUID> groups = items.stream()
                .map(ItemSnapshot::reconciliationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (groups.isEmpty()) return;
        // Clearing a partial group would leave the remaining lines tagged but unbalanced.
        List<UUID> groupItemIds = journalItemReconciliationPort.findItemIdsByReconciliationIds(List.copyOf(groups));
        journalItemReconciliationPort.clearReconciliation(
                groupItemIds.stream().map(JournalItemId::new).collect(Collectors.toList()));
    }

    private List<ItemSnapshot> loadAll(Set<UUID> ids) {
        UUID companyId = companyContextProvider.getObject().requireCompany().getId();
        List<ItemSnapshot> items = journalItemReconciliationPort.findItemsByIds(ids).stream()
                .filter(i -> companyId.equals(i.companyId()))
                .toList();
        if (items.size() != ids.size()) {
            throw new AccountingDomainException("error.accounting.journalItemNotFound", null,
                    "One or more journal lines were not found");
        }
        return items;
    }

    private void rejectTrade(ItemSnapshot item) {
        if (item.isTrade()) {
            throw new AccountingDomainException("error.accounting.reconcileTradeManaged", null,
                    "Receivable and payable lines are reconciled through payment allocations");
        }
    }
}
