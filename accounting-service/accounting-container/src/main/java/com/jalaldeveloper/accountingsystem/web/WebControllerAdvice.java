package com.jalaldeveloper.accountingsystem.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds companyId and requestURI to the model for all web dashboard views
 * (navbar badge and sidebar active state). requestURI is used because
 * Thymeleaf 3.1+ does not expose #request in templates by default.
 */
@ControllerAdvice(basePackages = "com.jalaldeveloper.accountingsystem.web")
public class WebControllerAdvice {

    private final WebCompanyContext companyContext;

    public WebControllerAdvice(WebCompanyContext companyContext) {
        this.companyContext = companyContext;
    }

    @ModelAttribute("companyId")
    public java.util.UUID companyId() {
        return companyContext.getCompanyId();
    }

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }
}
