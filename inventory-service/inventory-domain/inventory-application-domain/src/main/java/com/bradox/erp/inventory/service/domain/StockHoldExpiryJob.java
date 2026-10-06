package com.bradox.erp.inventory.service.domain;

import com.bradox.erp.inventory.service.domain.ports.input.StockHoldApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockHoldExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(StockHoldExpiryJob.class);

    private final StockHoldApplicationService stockHoldApplicationService;

    public StockHoldExpiryJob(StockHoldApplicationService stockHoldApplicationService) {
        this.stockHoldApplicationService = stockHoldApplicationService;
    }

    @Scheduled(fixedDelayString = "${delin.stock-hold.purge-delay-ms:300000}")
    public void purgeExpired() {
        int n = stockHoldApplicationService.purgeExpired();
        if (n > 0) {
            log.info("Purged {} expired stock holds", n);
        }
    }
}
