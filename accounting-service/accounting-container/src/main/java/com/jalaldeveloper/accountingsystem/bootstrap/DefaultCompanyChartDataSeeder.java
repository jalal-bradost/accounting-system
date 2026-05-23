package com.jalaldeveloper.accountingsystem.bootstrap;

import com.jalaldeveloper.accountingsystem.dataaccess.entity.AccountEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.entity.JournalEntity;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.AccountJpaRepository;
import com.jalaldeveloper.accountingsystem.dataaccess.repository.JournalJpaRepository;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.AccountType;
import com.jalaldeveloper.accountingsystem.domain.core.ValueObject.JournalType;
import com.jalaldeveloper.accountingsystem.web.DashboardController;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Inserts default chart of accounts and journals for the demo company when missing.
 */
@Component
@Order(0)
@ConditionalOnProperty(
        name = "accounting.seed.default-company-chart",
        havingValue = "true",
        matchIfMissing = true)
public class DefaultCompanyChartDataSeeder implements ApplicationRunner {

    private static final UUID COMPANY_ID = DashboardController.DEFAULT_COMPANY_ID;

    private final AccountJpaRepository accountJpaRepository;
    private final JournalJpaRepository journalJpaRepository;

    public DefaultCompanyChartDataSeeder(AccountJpaRepository accountJpaRepository,
                                         JournalJpaRepository journalJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.journalJpaRepository = journalJpaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAccounts();
        seedJournals();
    }

    private void seedAccounts() {
        insertAccount("430001", "Cash", AccountType.BANK_AND_CASH);
        insertAccount("430002", "Bank", AccountType.BANK_AND_CASH);
        insertAccount("430003", "Accounts Receivable", AccountType.RECEIVABLE);
        insertAccount("430004", "Accounts Payable", AccountType.PAYABLE);
        insertAccount("430005", "Sales Revenue", AccountType.INCOME);
        insertAccount("430006", "Sales Discount", AccountType.EXPENSES);
        insertAccount("430007", "Auction Fee Revenue", AccountType.INCOME);
        insertAccount("430008", "Delivery Fee Revenue", AccountType.INCOME);
        insertAccount("430009", "Cost of goods sold", AccountType.COST_OF_REVENUE);
        insertAccount("430010", "Inventory", AccountType.CURRENT_ASSETS);
        insertAccount("430011", "Stock Input (GR/IR)", AccountType.CURRENT_LIABILITIES);
        insertAccount("430012", "Stock Output", AccountType.CURRENT_ASSETS);
        insertAccount("430013", "Purchase VAT", AccountType.CURRENT_ASSETS);
        insertAccount("430014", "Exchange Gain", AccountType.OTHER_INCOME);
        insertAccount("430015", "Exchange Loss", AccountType.OTHER_EXPENSES);
    }

    private void insertAccount(String code, String name, AccountType type) {
        if (accountJpaRepository.existsByCompanyIdAndCode(COMPANY_ID, code)) {
            return;
        }
        AccountEntity e = new AccountEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(COMPANY_ID);
        e.setCode(code);
        e.setName(name);
        e.setType(type);
        e.setActive(true);
        accountJpaRepository.save(e);
    }

    private void seedJournals() {
        insertJournal("430001", "Cash", JournalType.CASH);
        insertJournal("430002", "Bank", JournalType.BANK);
        insertJournal("430003", "Sale", JournalType.SALE);
        insertJournal("430004", "Purchase", JournalType.PURCHASE);
        insertJournal("INV", "Inventory Valuation", JournalType.MISC);
    }

    private void insertJournal(String code, String name, JournalType type) {
        if (journalJpaRepository.existsByCompanyIdAndCode(COMPANY_ID, code)) {
            return;
        }
        JournalEntity e = new JournalEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(COMPANY_ID);
        e.setCode(code);
        e.setName(name);
        e.setType(type);
        journalJpaRepository.save(e);
    }
}
