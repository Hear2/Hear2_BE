package com.hear2.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.builder()
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(exception.getReason())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
