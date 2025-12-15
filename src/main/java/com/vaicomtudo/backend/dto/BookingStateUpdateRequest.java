package com.vaicomtudo.backend.dto;

import com.vaicomtudo.backend.data.entity.BookingState;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingStateUpdateRequest {
    
    @NotNull(message = "Booking state is required")
    private BookingState state;
}
