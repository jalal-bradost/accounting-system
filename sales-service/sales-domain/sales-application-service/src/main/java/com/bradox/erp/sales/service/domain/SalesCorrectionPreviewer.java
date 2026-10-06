package com.bradox.erp.sales.service.domain;

import com.bradox.erp.platform.transaction.DryRun;
import com.bradox.erp.sales.service.domain.dto.SalesCorrectionResult;
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
public class SalesCorrectionPreviewer {

    private final TransactionTemplate tx;

    public SalesCorrectionPreviewer(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    public SalesCorrectionResult preview(Supplier<SalesCorrectionResult> correction) {
        return DryRun.run(() -> tx.execute(status -> {
            status.setRollbackOnly();
            SalesCorrectionResult result = correction.get();
            result.setPreview(true);
            return result;
        }));
    }
}
