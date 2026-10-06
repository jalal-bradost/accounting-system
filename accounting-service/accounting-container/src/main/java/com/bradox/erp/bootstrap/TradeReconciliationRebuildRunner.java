package com.bradox.erp.bootstrap;

import com.bradox.erp.accounting.service.domain.CustomerPaymentService;
import com.bradox.erp.dataaccess.entity.AccTradeReconciliationRebuildEntity;
import com.bradox.erp.dataaccess.repository.AccTradeReconciliationRebuildJpaRepository;
import com.bradox.erp.platform.settings.CompanyApplicationService;
import com.bradox.erp.platform.settings.CompanyResponse;
import com.bradox.erp.purchase.service.domain.VendorPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * One-time per company+side rebuild of receivable/payable reconciliation tags from allocations.
 * Runs after Flyway and after demo seeders so migrated data gets tags without blocking startup.
 */
@Component
@Order(100)
public class TradeReconciliationRebuildRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TradeReconciliationRebuildRunner.class);

    public static final String SIDE_RECEIVABLE = "RECEIVABLE";
    public static final String SIDE_PAYABLE = "PAYABLE";

    private final CompanyApplicationService companyApplicationService;
    private final AccTradeReconciliationRebuildJpaRepository rebuildRepository;
    private final CustomerPaymentService customerPaymentService;
    private final VendorPaymentService vendorPaymentService;

    public TradeReconciliationRebuildRunner(CompanyApplicationService companyApplicationService,
                                            AccTradeReconciliationRebuildJpaRepository rebuildRepository,
                                            CustomerPaymentService customerPaymentService,
                                            VendorPaymentService vendorPaymentService) {
        this.companyApplicationService = companyApplicationService;
        this.rebuildRepository = rebuildRepository;
        this.customerPaymentService = customerPaymentService;
        this.vendorPaymentService = vendorPaymentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (CompanyResponse company : companyApplicationService.listAll()) {
            UUID companyId = company.id();
            rebuildSide(companyId, company.name(), SIDE_RECEIVABLE, () -> customerPaymentService.rebuildCompany(companyId));
            rebuildSide(companyId, company.name(), SIDE_PAYABLE, () -> vendorPaymentService.rebuildCompany(companyId));
        }
    }

    private void rebuildSide(UUID companyId, String companyName, String side, Runnable rebuild) {
        if (rebuildRepository.existsByCompanyIdAndSide(companyId, side)) {
            return;
        }
        try {
            log.info("Rebuilding {} trade reconciliation for company {} ({})", side, companyName, companyId);
            rebuild.run();
            rebuildRepository.save(new AccTradeReconciliationRebuildEntity(companyId, side, Instant.now()));
            log.info("Finished {} trade reconciliation rebuild for company {}", side, companyId);
        } catch (Exception ex) {
            log.error("Failed {} trade reconciliation rebuild for company {} ({}): {}",
                    side, companyName, companyId, ex.getMessage(), ex);
        }
    }
}
