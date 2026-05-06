package com.jalaldeveloper.accountingsystem.platform.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation used by {@link CurrentCompanyArgumentResolver} to inject the
 * current {@link com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId}
 * into a controller method parameter.
 *
 * <p>If {@link #required()} is {@code true} (the default) and no company is on the
 * request, the resolver throws an {@link IllegalStateException}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentCompany {
    boolean required() default true;
}
