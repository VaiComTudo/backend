package com.vaicomtudo.backend.exception;

public class UnauthorizedBookingAccessException extends RuntimeException {
    public UnauthorizedBookingAccessException() {
        super("You are not authorized to access this booking");
    }
    
    public UnauthorizedBookingAccessException(String message) {
        super(message);
    }
}
