package com.bradox.erp.contacts.application.rest;

import com.bradox.erp.application.handler.ErrorDTO;
import com.bradox.erp.application.handler.ExceptionMessageResolver;
import com.bradox.erp.contacts.domain.core.exception.ContactsDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContactsExceptionHandler {

    private final ExceptionMessageResolver messageResolver;

    public ContactsExceptionHandler(ExceptionMessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(ContactsDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(ContactsDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("CONTACTS_DOMAIN_ERROR")
                .message(messageResolver.resolve(ex))
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }
}
