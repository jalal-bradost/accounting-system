package com.bradox.erp.purchase.service.domain;

import com.bradox.erp.platform.transaction.DryRun;
import com.bradox.erp.purchase.service.domain.dto.PurchaseCorrectionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Exact previews of guided corrections: the correction runs for real inside a transaction that is
 * always rolled back, so the preview shows precisely the documents and amounts it would post.
 * Nothing is kept: data, numbering and (via {@link DryRun}) the audit log are untouched.
 */
@Service
public class PurchaseCorrectionPreviewer {

    private final TransactionTemplate tx;

    public PurchaseCorrectionPreviewer(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    public PurchaseCorrectionResult preview(Supplier<PurchaseCorrectionResult> correction) {
        return DryRun.run(() -> tx.execute(status -> {
            status.setRollbackOnly();
            PurchaseCorrectionResult result = correction.get();
            result.setPreview(true);
            return result;
        }));
    }
}
