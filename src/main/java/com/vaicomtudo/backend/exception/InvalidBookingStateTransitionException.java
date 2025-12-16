package com.vaicomtudo.backend.exception;

public class InvalidBookingStateTransitionException extends RuntimeException {
    public InvalidBookingStateTransitionException() {
        super("Invalid booking state transition");
    }
    
    public InvalidBookingStateTransitionException(String message) {
        super(message);
    }
}
