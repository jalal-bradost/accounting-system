package com.bradox.erp.documents.application.rest;

import com.bradox.erp.application.handler.ErrorDTO;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(basePackageClasses = DocumentController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentsExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDTO> tooLarge(MaxUploadSizeExceededException ex) {
        ErrorDTO dto = ErrorDTO.builder()
                .code("PAYLOAD_TOO_LARGE")
                .message("The file is larger than the allowed upload size")
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(dto);
    }
}
