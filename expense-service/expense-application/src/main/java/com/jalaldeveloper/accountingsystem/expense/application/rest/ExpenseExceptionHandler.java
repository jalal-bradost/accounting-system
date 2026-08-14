package com.jalaldeveloper.accountingsystem.expense.application.rest;

import com.jalaldeveloper.accountingsystem.application.handler.ErrorDTO;
import com.jalaldeveloper.accountingsystem.domain.core.exception.AccountingDomainException;
import com.jalaldeveloper.accountingsystem.expense.domain.core.exception.ExpenseDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExpenseExceptionHandler {

    @ExceptionHandler(ExpenseDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(ExpenseDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("EXPENSE_DOMAIN_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(AccountingDomainException.class)
    public ResponseEntity<ErrorDTO> handleAccounting(AccountingDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("ACCOUNTING_DOMAIN_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        ErrorDTO dto = ErrorDTO.builder()
                .code("VALIDATION_ERROR")
                .message(message)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(dto);
    }
}
