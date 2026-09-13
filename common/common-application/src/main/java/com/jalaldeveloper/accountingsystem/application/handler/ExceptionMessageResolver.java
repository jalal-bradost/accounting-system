package com.jalaldeveloper.accountingsystem.application.handler;

import com.jalaldeveloper.accountingsystem.domain.exception.DomainException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ExceptionMessageResolver {

    private final MessageSource messageSource;

    public ExceptionMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolve(DomainException ex) {
        return resolve(ex, LocaleContextHolder.getLocale());
    }

    public String resolve(DomainException ex, Locale locale) {
        String key = ex.getMessageKey();
        if (key != null && !key.isBlank()) {
            return messageSource.getMessage(key, ex.getMessageArgs(), ex.getMessage(), locale);
        }
        return ex.getMessage();
    }

    public String resolve(String messageKey, Object[] args, String defaultMessage) {
        return messageSource.getMessage(messageKey, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    public String resolve(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        return messageSource.getMessage(messageKey, args, defaultMessage, locale);
    }
}
