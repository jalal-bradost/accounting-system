package com.bradox.delin.purchase.application.rest;

import com.bradox.delin.application.handler.ErrorDTO;
import com.bradox.delin.application.handler.ExceptionMessageResolver;
import com.bradox.delin.purchase.domain.core.PurchaseDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PurchaseExceptionHandler {

    private final ExceptionMessageResolver messageResolver;

    public PurchaseExceptionHandler(ExceptionMessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(PurchaseDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(PurchaseDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("PURCHASE_DOMAIN_ERROR")
                .message(messageResolver.resolve(ex))
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDTO> handleOptimisticLock(OptimisticLockingFailureException ex) {
        String defaultMessage = "Document was modified by another transaction; please retry";
        ErrorDTO dto = ErrorDTO.builder()
                .code("PURCHASE_CONCURRENT_UPDATE")
                .message(messageResolver.resolve("error.concurrent.optimisticLock", null, defaultMessage))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(dto);
    }
}
