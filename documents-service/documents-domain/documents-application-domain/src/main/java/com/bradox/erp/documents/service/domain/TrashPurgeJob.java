package com.bradox.erp.documents.service.domain;

import com.bradox.erp.documents.service.domain.ports.input.DocumentApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Permanently deletes documents that have been in the trash for 30 days, unless under retention. */
@Component
class TrashPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(TrashPurgeJob.class);

    private final DocumentApplicationService documents;

    TrashPurgeJob(DocumentApplicationService documents) {
        this.documents = documents;
    }

    @Scheduled(cron = "${app.documents.purge-cron:0 30 3 * * *}")
    void purge() {
        try {
            int purged = documents.purgeExpiredTrash();
            if (purged > 0) {
                log.info("Documents trash purge removed {} documents", purged);
            }
        } catch (RuntimeException e) {
            log.error("Documents trash purge failed", e);
        }
    }
}
