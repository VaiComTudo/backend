package com.vaicomtudo.backend.errors;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vaicomtudo.backend.exception.BookingNotFoundException;
import com.vaicomtudo.backend.exception.InvalidBookingDatesException;
import com.vaicomtudo.backend.exception.InvalidBookingStateTransitionException;
import com.vaicomtudo.backend.exception.ListingNotFoundException;
import com.vaicomtudo.backend.exception.UnauthorizedBookingAccessException;
import com.vaicomtudo.backend.exception.UnauthorizedListingAccessException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {

    private String error = "String";
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AuthorizationDeniedException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                error, "Access Denied"
            ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                error, "Invalid credentials"
            ));
    }

    @ExceptionHandler(ListingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleListingNotFound(ListingNotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(UnauthorizedListingAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedListingAccess(UnauthorizedListingAccessException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBookingNotFound(BookingNotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(UnauthorizedBookingAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedBookingAccess(UnauthorizedBookingAccessException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(InvalidBookingDatesException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBookingDates(InvalidBookingDatesException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(InvalidBookingStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBookingStateTransition(InvalidBookingStateTransitionException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                error, e.getMessage()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                error, e.getMessage()
            ));
    }
}

