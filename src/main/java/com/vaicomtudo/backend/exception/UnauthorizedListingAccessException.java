package com.vaicomtudo.backend.exception;

public class UnauthorizedListingAccessException extends RuntimeException {
    public UnauthorizedListingAccessException() {
        super("You are not authorized to access this listing");
    }
    
    public UnauthorizedListingAccessException(String message) {
        super(message);
    }
}
