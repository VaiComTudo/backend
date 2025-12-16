package com.vaicomtudo.backend.boundary;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaicomtudo.backend.service.FakePaymentService;
import com.vaicomtudo.backend.service.FakePaymentService.BookingPaymentStatus;
import com.vaicomtudo.backend.service.FakePaymentService.PaymentInfo;

import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping("/api/v1/payments")
public class FakePaymentController {

    private final FakePaymentService paymentService;

    public FakePaymentController(FakePaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record CompletePaymentRequest(BigDecimal rentalFee, BigDecimal deposit) {
    }

    public record PaymentResponse(
        UUID bookingId,
        String renterEmail,
        BigDecimal rentalFee,
        BigDecimal deposit,
        BigDecimal total,
        BookingPaymentStatus status,
        String receiptMessage
    ) {
    }

    @PostMapping("/bookings/{bookingId}/complete")
    @Timed(value = "request.payments.complete")
    public ResponseEntity<PaymentResponse> completePayment(
        @PathVariable UUID bookingId,
        @RequestBody CompletePaymentRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String renterEmail = authentication.getName();

        PaymentInfo info = paymentService.completePayment(
            bookingId,
            renterEmail,
            request.rentalFee(),
            request.deposit()
        );

        return ResponseEntity.ok(toResponse(info));
    }

    @GetMapping("/bookings/{bookingId}")
    @Timed(value = "request.payments.get")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID bookingId) {
        PaymentInfo info = paymentService.getPayment(bookingId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(info));
    }

    private PaymentResponse toResponse(PaymentInfo info) {
        return new PaymentResponse(
            info.getBookingId(),
            info.getRenterEmail(),
            info.getRentalFee(),
            info.getDeposit(),
            info.getTotal(),
            info.getStatus(),
            info.getReceiptMessage()
        );
    }
}

