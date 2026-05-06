package com.jalaldeveloper.accountingsystem.platform.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for entity fields that should be captured by {@link AuditingEntityListener}
 * on CREATE / UPDATE / DELETE. Any field NOT annotated is ignored, so we skip
 * noisy bookkeeping fields like timestamps and version columns.
 *
 * <p>Use the optional {@link #name()} to override the field name in the audit JSON.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditTrack {
    String name() default "";
}
