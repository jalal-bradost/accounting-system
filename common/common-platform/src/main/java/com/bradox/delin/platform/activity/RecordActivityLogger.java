package com.bradox.delin.platform.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Best-effort chatter posts for business records. Failures never abort the domain transaction.
 */
@Component
public class RecordActivityLogger {

    private static final Logger log = LoggerFactory.getLogger(RecordActivityLogger.class);

    public static final String MODEL_PURCHASE_ORDER = "purchase.order";
    public static final String MODEL_VENDOR_BILL = "purchase.vendor.bill";
    public static final String MODEL_VENDOR_PAYMENT = "purchase.vendor.payment";
    public static final String MODEL_SALES_ORDER = "sales.order";
    public static final String MODEL_CUSTOMER_INVOICE = "accounting.customer.invoice";
    public static final String MODEL_CUSTOMER_PAYMENT = "accounting.customer.payment";

    private final ActivityApplicationService activityService;

    public RecordActivityLogger(ActivityApplicationService activityService) {
        this.activityService = activityService;
    }

    public void log(UUID companyId, String modelName, UUID recordId, String message) {
        if (companyId == null || modelName == null || recordId == null || message == null || message.isBlank()) {
            return;
        }
        try {
            CreateActivityCommand cmd = new CreateActivityCommand();
            cmd.setCompanyId(companyId);
            cmd.setModelName(modelName);
            cmd.setRecordId(recordId);
            cmd.setKind(ActivityKind.SYSTEM);
            cmd.setSubject(message.length() > 120 ? message.substring(0, 117) + "…" : message);
            cmd.setBody(message);
            activityService.create(cmd);
        } catch (Exception e) {
            log.warn("Failed to post chatter activity for {}/{}: {}", modelName, recordId, e.getMessage());
        }
    }

    public void logFieldChange(UUID companyId, String modelName, UUID recordId,
                               String oldValue, String newValue, String fieldLabel) {
        log(companyId, modelName, recordId,
                String.valueOf(oldValue) + " → " + String.valueOf(newValue) + " (" + fieldLabel + ")");
    }
}
