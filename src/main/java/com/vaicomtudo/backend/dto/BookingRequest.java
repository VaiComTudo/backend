package com.vaicomtudo.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
    
    @NotNull(message = "Listing ID is required")
    private UUID listingId;
    
    @NotNull(message = "Pickup date and time is required")
    @Future(message = "Pickup date must be in the future")
    private LocalDateTime pickupDateTime;
    
    @NotNull(message = "Dropoff date and time is required")
    @Future(message = "Dropoff date must be in the future")
    private LocalDateTime dropoffDateTime;
}
