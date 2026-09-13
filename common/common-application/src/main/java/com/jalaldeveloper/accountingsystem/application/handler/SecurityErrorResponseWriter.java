package com.jalaldeveloper.accountingsystem.application.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;

@Component
public class SecurityErrorResponseWriter {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public SecurityErrorResponseWriter(MessageSource messageSource, LocaleResolver localeResolver) {
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    public void writeJsonError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String messageKey,
            String defaultMessage)
            throws IOException {
        Locale locale = localeResolver.resolveLocale(request);
        String message = messageSource.getMessage(messageKey, null, defaultMessage, locale);
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(toJsonError(code, message));
    }

    private static String toJsonError(String code, String message) {
        return "{\"code\":\"" + escapeJson(code) + "\",\"message\":\"" + escapeJson(message) + "\"}";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
