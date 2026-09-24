package com.bradox.erp.expense.application.rest;

import com.bradox.erp.application.handler.ErrorDTO;
import com.bradox.erp.application.handler.ExceptionMessageResolver;
import com.bradox.erp.domain.core.exception.AccountingDomainException;
import com.bradox.erp.expense.domain.core.exception.ExpenseDomainException;
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

    private final ExceptionMessageResolver messageResolver;

    public ExpenseExceptionHandler(ExceptionMessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(ExpenseDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(ExpenseDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("EXPENSE_DOMAIN_ERROR")
                .message(messageResolver.resolve(ex))
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(AccountingDomainException.class)
    public ResponseEntity<ErrorDTO> handleAccounting(AccountingDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("ACCOUNTING_DOMAIN_ERROR")
                .message(messageResolver.resolve(ex))
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
