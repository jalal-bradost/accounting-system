package com.bradox.delin.pos.application.rest;

import com.bradox.delin.application.handler.ErrorDTO;
import com.bradox.delin.application.handler.ExceptionMessageResolver;
import com.bradox.delin.pos.domain.core.PosDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PosExceptionHandler {

    private final ExceptionMessageResolver messageResolver;

    public PosExceptionHandler(ExceptionMessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(PosDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(PosDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("POS_DOMAIN_ERROR")
                .message(messageResolver.resolve(ex))
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDTO> handleOptimisticLock(OptimisticLockingFailureException ex) {
        String defaultMessage = "POS document was modified by another transaction; please retry";
        ErrorDTO dto = ErrorDTO.builder()
                .code("POS_CONCURRENT_UPDATE")
                .message(messageResolver.resolve("error.concurrent.optimisticLockPos", null, defaultMessage))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(dto);
    }
}
