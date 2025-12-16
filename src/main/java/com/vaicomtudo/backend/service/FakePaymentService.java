package com.vaicomtudo.backend.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class FakePaymentService {

    public enum BookingPaymentStatus {
        PENDING,
        CONFIRMED
    }

    public static class PaymentInfo {
        private UUID bookingId;
        private String renterEmail;
        private BigDecimal rentalFee;
        private BigDecimal deposit;
        private BigDecimal total;
        private BookingPaymentStatus status;
        private String receiptMessage;

        public UUID getBookingId() {
            return bookingId;
        }

        public void setBookingId(UUID bookingId) {
            this.bookingId = bookingId;
        }

        public String getRenterEmail() {
            return renterEmail;
        }

        public void setRenterEmail(String renterEmail) {
            this.renterEmail = renterEmail;
        }

        public BigDecimal getRentalFee() {
            return rentalFee;
        }

        public void setRentalFee(BigDecimal rentalFee) {
            this.rentalFee = rentalFee;
        }

        public BigDecimal getDeposit() {
            return deposit;
        }

        public void setDeposit(BigDecimal deposit) {
            this.deposit = deposit;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }

        public BookingPaymentStatus getStatus() {
            return status;
        }

        public void setStatus(BookingPaymentStatus status) {
            this.status = status;
        }

        public String getReceiptMessage() {
            return receiptMessage;
        }

        public void setReceiptMessage(String receiptMessage) {
            this.receiptMessage = receiptMessage;
        }
    }

    private final Map<UUID, PaymentInfo> payments = new ConcurrentHashMap<>();

    public PaymentInfo completePayment(UUID bookingId, String renterEmail, BigDecimal rentalFee, BigDecimal deposit) {
        PaymentInfo info = payments.computeIfAbsent(bookingId, id -> {
            PaymentInfo p = new PaymentInfo();
            p.setBookingId(id);
            p.setStatus(BookingPaymentStatus.PENDING);
            return p;
        });

        info.setRenterEmail(renterEmail);
        info.setRentalFee(rentalFee);
        info.setDeposit(deposit);
        info.setTotal(rentalFee.add(deposit));
        info.setStatus(BookingPaymentStatus.CONFIRMED);
        info.setReceiptMessage("A digital receipt was sent to " + renterEmail);

        return info;
    }

    public PaymentInfo getPayment(UUID bookingId) {
        return payments.get(bookingId);
    }
}

