package com.jalaldeveloper.accountingsystem.application.handler;

import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountingExceptionHandler {

    @ExceptionHandler(AccountingDomainException.class)
    public ResponseEntity<ErrorDTO> handleAccountingDomainException(AccountingDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("ACCOUNTING_DOMAIN_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(dto);
    }
}
