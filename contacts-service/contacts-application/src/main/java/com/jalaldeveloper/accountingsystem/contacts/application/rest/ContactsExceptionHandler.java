package com.jalaldeveloper.accountingsystem.contacts.application.rest;

import com.jalaldeveloper.accountingsystem.application.handler.ErrorDTO;
import com.jalaldeveloper.accountingsystem.contacts.domain.core.exception.ContactsDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContactsExceptionHandler {

    @ExceptionHandler(ContactsDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(ContactsDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("CONTACTS_DOMAIN_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }
}
