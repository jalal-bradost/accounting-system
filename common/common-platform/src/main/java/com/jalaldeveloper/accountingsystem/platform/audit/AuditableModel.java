package com.jalaldeveloper.accountingsystem.platform.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level marker declaring the canonical {@code modelName} used in audit log
 * entries (e.g. {@code "partner"}, {@code "product"}, {@code "stock.picking"}).
 * Required on entities that opt in to {@link AuditingEntityListener} CRUD capture.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableModel {
    String value();
}
