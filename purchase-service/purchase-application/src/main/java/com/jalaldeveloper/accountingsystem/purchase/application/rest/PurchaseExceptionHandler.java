package com.jalaldeveloper.accountingsystem.purchase.application.rest;

import com.jalaldeveloper.accountingsystem.application.handler.ErrorDTO;
import com.jalaldeveloper.accountingsystem.purchase.domain.core.PurchaseDomainException;
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

    @ExceptionHandler(PurchaseDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(PurchaseDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("PURCHASE_DOMAIN_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDTO> handleOptimisticLock(OptimisticLockingFailureException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("PURCHASE_CONCURRENT_UPDATE")
                .message("Document was modified by another transaction; please retry")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(dto);
    }
}
