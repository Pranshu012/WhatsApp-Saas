package com.example.wasaas.common.exception;

import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handleDomainException(DomainException exception) {
        var status = exception.status();
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                exception.getMessage(), MDC.get("requestId")));
    }
}
