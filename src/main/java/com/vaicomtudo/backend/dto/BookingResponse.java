package com.vaicomtudo.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vaicomtudo.backend.data.entity.BookingState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID listingId;
    private String listingTitle;
    private UUID renterId;
    private String renterName;
    private UUID ownerId;
    private String ownerName;
    private LocalDateTime pickupDateTime;
    private LocalDateTime dropoffDateTime;
    private BookingState state;
}
