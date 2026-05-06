package com.jalaldeveloper.accountingsystem.inventory.application.rest;

import com.jalaldeveloper.accountingsystem.application.handler.ErrorDTO;
import com.jalaldeveloper.accountingsystem.inventory.domain.core.exception.InventoryDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InventoryExceptionHandler {

    @ExceptionHandler(InventoryDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(InventoryDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("INVENTORY_DOMAIN_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDTO> handleOptimisticLock(OptimisticLockingFailureException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("INVENTORY_CONCURRENT_UPDATE")
                .message("Stock has been modified by another transaction; please retry the operation")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(dto);
    }
}
