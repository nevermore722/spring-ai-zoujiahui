package org.zjh.ai.springaizoujiahui.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zjh.ai.springaizoujiahui.security.SensitiveContentException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class SensitiveExceptionHandler {

    @ExceptionHandler(SensitiveContentException.class)
    public ResponseEntity<String> onSensitive(SensitiveContentException ex) {
        log.info("Sensitive input blocked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.TEXT_PLAIN)
                .body(ex.getMessage());
    }
}

