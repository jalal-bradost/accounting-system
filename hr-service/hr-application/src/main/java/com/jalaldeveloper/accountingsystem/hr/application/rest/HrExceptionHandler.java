package com.jalaldeveloper.accountingsystem.hr.application.rest;

import com.jalaldeveloper.accountingsystem.application.handler.ErrorDTO;
import com.jalaldeveloper.accountingsystem.hr.domain.core.exception.HrDomainException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HrExceptionHandler {

    @ExceptionHandler(HrDomainException.class)
    public ResponseEntity<ErrorDTO> handleDomain(HrDomainException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("HR_DOMAIN_ERROR")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(dto);
    }
}
