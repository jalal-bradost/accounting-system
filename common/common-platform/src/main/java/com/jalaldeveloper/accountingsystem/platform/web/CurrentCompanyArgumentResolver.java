package com.jalaldeveloper.accountingsystem.platform.web;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves controller parameters annotated {@link CurrentCompany} into a {@link CompanyId}
 * read from {@link CompanyContext} (backed by the current servlet request).
 */
@Component
public class CurrentCompanyArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectProvider<CompanyContext> companyContextProvider;

    public CurrentCompanyArgumentResolver(ObjectProvider<CompanyContext> companyContextProvider) {
        this.companyContextProvider = companyContextProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentCompany.class) != null
                && CompanyId.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mav,
                                  NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        CurrentCompany annotation = parameter.getParameterAnnotation(CurrentCompany.class);
        boolean required = annotation == null || annotation.required();
        CompanyContext context = companyContextProvider.getObject();
        if (required) {
            return context.requireCompany();
        }
        return context.currentCompany().orElse(null);
    }
}
