package com.jalaldeveloper.accountingsystem.web;

import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles exceptions from web dashboard controllers and redirects with flash error
 * so the user sees a message instead of JSON or stack trace.
 */
@ControllerAdvice(basePackages = "com.jalaldeveloper.accountingsystem.web")
public class WebExceptionHandler {

    @ExceptionHandler(AccountingDomainException.class)
    public String handleAccountingDomainException(AccountingDomainException ex,
                                                  RedirectAttributes redirectAttributes,
                                                  HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return redirectBack(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex,
                                                 RedirectAttributes redirectAttributes,
                                                 HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return redirectBack(request);
    }

    private static String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/web")) {
            try {
                return "redirect:" + new java.net.URL(referer).getPath();
            } catch (Exception ignored) {
            }
        }
        return "redirect:/web";
    }
}
