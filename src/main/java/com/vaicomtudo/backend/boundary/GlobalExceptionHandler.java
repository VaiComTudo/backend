package com.vaicomtudo.backend.boundary;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.vaicomtudo.backend.exception.EmailNotFoundException;
import com.vaicomtudo.backend.exception.MismatchEmailException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MismatchEmailException.class)
    public ResponseEntity<String> handleMismatchIDException(MismatchEmailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email mismatch");
    }

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<String> handleIllegalArgumentException(EmailNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found");
    }
}
