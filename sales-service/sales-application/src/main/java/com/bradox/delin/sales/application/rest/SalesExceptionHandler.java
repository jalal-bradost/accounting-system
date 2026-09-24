package com.bradox.delin.sales.application.rest;

import com.bradox.delin.application.handler.ErrorDTO;
import com.bradox.delin.application.handler.ExceptionMessageResolver;
import com.bradox.delin.sales.domain.core.SalesDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SalesExceptionHandler {

    private final ExceptionMessageResolver messageResolver;

    public SalesExceptionHandler(ExceptionMessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(SalesDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(SalesDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("SALES_DOMAIN_ERROR")
                .message(messageResolver.resolve(ex))
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDTO> handleOptimisticLock(OptimisticLockingFailureException ex) {
        String defaultMessage = "Document was modified by another transaction; please retry";
        ErrorDTO dto = ErrorDTO.builder()
                .code("SALES_CONCURRENT_UPDATE")
                .message(messageResolver.resolve("error.concurrent.optimisticLock", null, defaultMessage))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(dto);
    }
}
