package com.jalaldeveloper.accountingsystem.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level RBAC guard. {@link PermissionAspect} intercepts annotated methods
 * and delegates the check to {@link AuthorizationPort}. Fails with
 * {@link ForbiddenException} when the check returns false.
 *
 * <p>Permission codes follow the {@code module.aggregate.action} convention,
 * e.g. {@code contacts.partner.write}, {@code inventory.picking.confirm}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    String[] value();

    LogicalOp op() default LogicalOp.AND;

    enum LogicalOp { AND, OR }
}
