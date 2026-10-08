package com.bradox.erp.sign.service.domain;

import com.bradox.erp.sign.domain.core.entity.SignRequest;
import com.bradox.erp.sign.domain.core.valueobject.EventType;
import com.bradox.erp.sign.service.domain.ports.output.repository.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.util.Map;

/**
 * Background work of Sign (SIG-05, SIG-06). Every job can run twice with the same result. Reminders need no job: whether a
 * "Remind" prompt is due is computed when a request is shown (phase 1 sends nothing automatically).
 */
@Component
public class SignJobs {

    private static final Logger log = LoggerFactory.getLogger(SignJobs.class);

    private final RequestRepository requests;
    private final RequestFinalizer finalizer;
    private final SignRecorder recorder;
    private final SignAccess access;
    private final TransactionOperations tx;

    SignJobs(RequestRepository requests, RequestFinalizer finalizer, SignRecorder recorder, SignAccess access, TransactionOperations tx) {
        this.requests = requests;
        this.finalizer = finalizer;
        this.recorder = recorder;
        this.access = access;
        this.tx = tx;
    }

    /** Daily: open requests past their expiry become EXPIRED, their links stop working, the sender is told (SIG-06 #2). */
    @Scheduled(cron = "${app.sign.expiry-cron:0 15 2 * * *}")
    public int expireOverdue() {
        Instant now = access.now();
        int count = 0;
        for (SignRequest candidate : requests.findLiveExpiredBefore(now)) {
            Boolean changed = tx.execute(status -> {
                SignRequest r = requests.findAny(candidate.getId()).orElse(null);
                if (r == null || !r.expire(now)) {
                    return false;
                }
                requests.save(r);
                recorder.event(r.getId(), null, EventType.EXPIRED, now, null, null, "Expired");
                recorder.audit(r.getCompanyId(), r.getId(), "Signature request expired", Map.of());
                finalizer.notifyOwner(r, "Expired: " + r.getName(), "Nobody finished signing before the deadline, so the links no longer work.");
                return true;
            });
            if (Boolean.TRUE.equals(changed)) {
                count++;
            }
        }
        return count;
    }

    /** Every 10 minutes: builds final PDFs that failed or were interrupted, up to 3 attempts (SIG-05 #7). */
    @Scheduled(fixedDelayString = "${app.sign.finalize-retry-ms:600000}", initialDelayString = "${app.sign.finalize-retry-ms:600000}")
    public int retryFinalization() {
        int count = 0;
        for (SignRequest r : requests.findAwaitingFinal()) {
            if (r.canRetryBuild()) {
                finalizer.finalizeRequest(r.getId());
                count++;
            }
        }
        return count;
    }

    /** Nightly: a COMPLETED request must have a final document (reliability NFR). Reports, never changes. */
    @Scheduled(cron = "${app.sign.orphan-cron:0 45 3 * * *}")
    public int checkOrphans() {
        int count = 0;
        for (SignRequest r : requests.findCompletedWithoutFinal()) {
            log.error("Sign request {} is COMPLETED without a final document", r.getId());
            recorder.audit(r.getCompanyId(), r.getId(), "Integrity check: completed without final document", Map.of());
            count++;
        }
        return count;
    }
}
