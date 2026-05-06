package com.jalaldeveloper.accountingsystem.platform.audit;

/**
 * Type of mutation captured by the audit log. CRUD actions are produced automatically
 * by {@link AuditingEntityListener}; {@link #BUSINESS_EVENT} is used for explicit
 * application-level audit events ("credit limit raised", "stock picking confirmed").
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    ARCHIVE,
    UNARCHIVE,
    BUSINESS_EVENT
}
