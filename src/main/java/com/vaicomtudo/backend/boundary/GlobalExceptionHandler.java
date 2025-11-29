package com.vaicomtudo.backend.boundary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.vaicomtudo.backend.exception.MismatchIDException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MismatchIDException.class)
    public ResponseEntity<String> handleMismatchIDException(MismatchIDException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID mismatch");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
