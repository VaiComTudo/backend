package com.vaicomtudo.backend.boundary;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.dto.BookingRequest;
import com.vaicomtudo.backend.dto.BookingResponse;
import com.vaicomtudo.backend.dto.BookingStateUpdateRequest;
import com.vaicomtudo.backend.service.BookingService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Timed(value = "request.createbooking")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        BookingResponse booking = bookingService.createBooking(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/renter")
    @Timed(value = "request.getrenterbookings")
    public ResponseEntity<List<BookingResponse>> getRenterBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        List<BookingResponse> bookings = bookingService.getRenterBookings(userEmail);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/owner")
    @Timed(value = "request.getownerbookings")
    public ResponseEntity<List<BookingResponse>> getOwnerBookings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        List<BookingResponse> bookings = bookingService.getOwnerBookings(userEmail);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{bookingId}")
    @Timed(value = "request.getbooking")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        BookingResponse booking = bookingService.getBooking(bookingId, userEmail);
        return ResponseEntity.ok(booking);
    }

    @PatchMapping("/{bookingId}/state")
    @Timed(value = "request.updatebookingstate")
    public ResponseEntity<BookingResponse> updateBookingState(
        @PathVariable UUID bookingId,
        @Valid @RequestBody BookingStateUpdateRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        BookingResponse booking = bookingService.updateBookingState(bookingId, request.getState(), userEmail);
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/{bookingId}")
    @Timed(value = "request.cancelbooking")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        bookingService.cancelBooking(bookingId, userEmail);
        return ResponseEntity.noContent().build();
    }
}
