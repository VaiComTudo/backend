package com.vaicomtudo.backend.exception;

public class InvalidBookingDatesException extends RuntimeException {
    public InvalidBookingDatesException() {
        super("Dropoff date must be after pickup date");
    }
    
    public InvalidBookingDatesException(String message) {
        super(message);
    }
}
