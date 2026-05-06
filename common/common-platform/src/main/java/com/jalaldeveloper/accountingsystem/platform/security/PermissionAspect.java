package com.jalaldeveloper.accountingsystem.platform.security;

import com.jalaldeveloper.accountingsystem.domain.valueobject.UserId;
import com.jalaldeveloper.accountingsystem.platform.web.CompanyContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring AOP aspect enforcing {@link RequiresPermission} method/class annotations.
 * Reads the current {@link UserId} from {@link CompanyContext}
 * and consults the {@link AuthorizationPort}. If the request has no current user
 * (e.g. dev mode without auth), the default permissive port returns true.
 */
@Aspect
@Component
public class PermissionAspect {

    private final AuthorizationPort authorizationPort;
    private final ObjectProvider<CompanyContext> companyContextProvider;

    public PermissionAspect(AuthorizationPort authorizationPort,
                            ObjectProvider<CompanyContext> companyContextProvider) {
        this.authorizationPort = authorizationPort;
        this.companyContextProvider = companyContextProvider;
    }

    @Around("@annotation(com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission) "
            + "|| @within(com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission)")
    public Object enforce(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        RequiresPermission annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequiresPermission.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RequiresPermission.class);
        }
        if (annotation == null) {
            return pjp.proceed();
        }

        Set<String> required = Arrays.stream(annotation.value())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
        if (required.isEmpty()) {
            return pjp.proceed();
        }

        CompanyContext context = companyContextProvider.getIfAvailable();
        UserId userId = context != null ? context.currentUser().orElse(null) : null;

        boolean allowed = switch (annotation.op()) {
            case AND -> authorizationPort.hasAll(userId, required);
            case OR -> authorizationPort.hasAny(userId, required);
        };

        if (!allowed) {
            throw new ForbiddenException("Missing required permission(s): " + required);
        }
        return pjp.proceed();
    }
}
