package com.jalaldeveloper.accountingsystem.platform.application.rest;

import com.jalaldeveloper.accountingsystem.application.exception.LocalizedResponseStatusException;
import com.jalaldeveloper.accountingsystem.application.handler.ErrorDTO;
import com.jalaldeveloper.accountingsystem.application.handler.ExceptionMessageResolver;
import com.jalaldeveloper.accountingsystem.platform.security.ForbiddenException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformExceptionHandler {

    private final ExceptionMessageResolver messageResolver;

    public PlatformExceptionHandler(ExceptionMessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorDTO> handleForbidden(ForbiddenException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("FORBIDDEN")
                .message(messageResolver.resolve(ex))
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(dto);
    }

    @ExceptionHandler(LocalizedResponseStatusException.class)
    public ResponseEntity<ErrorDTO> handleLocalizedStatus(LocalizedResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = messageResolver.resolve(ex.getMessageKey(), ex.getMessageArgs(), ex.getReason());
        String code = status.is4xxClientError() ? status.name() : "ERROR";
        ErrorDTO dto = ErrorDTO.builder().code(code).message(message).build();
        return ResponseEntity.status(status).body(dto);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDTO> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = status.is4xxClientError() ? status.name() : "ERROR";
        ErrorDTO dto = ErrorDTO.builder()
                .code(code)
                .message(ex.getReason() != null ? ex.getReason() : status.getReasonPhrase())
                .build();
        return ResponseEntity.status(status).body(dto);
    }
}
