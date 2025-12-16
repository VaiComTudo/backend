package com.vaicomtudo.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaicomtudo.backend.data.entity.Payment;
import com.vaicomtudo.backend.data.entity.Payment.PaymentStatus;
import com.vaicomtudo.backend.data.entity.User;
import com.vaicomtudo.backend.data.repository.PaymentRepository;
import com.vaicomtudo.backend.data.repository.UserRepository;

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

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public FakePaymentService(PaymentRepository paymentRepository, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentInfo completePayment(UUID bookingId, String renterEmail, BigDecimal rentalFee, BigDecimal deposit) {
        BigDecimal total = rentalFee.add(deposit);
        LocalDateTime now = LocalDateTime.now();

        Optional<Payment> existingPayment = paymentRepository.findByBookingId(bookingId);
        Payment payment;

        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
            payment.setRentalFee(rentalFee);
            payment.setDeposit(deposit);
            payment.setTotal(total);
            payment.setStatus(PaymentStatus.CONFIRMED);
            payment.setReceiptMessage("A digital receipt was sent to " + renterEmail);
            payment.setUpdatedAt(now);
        } else {
            Optional<User> renterUser = userRepository.findByAccountEmail(renterEmail);
            
            payment = Payment.builder()
                .bookingId(bookingId)
                .renter(renterUser.orElse(null))
                .renterEmail(renterEmail)
                .rentalFee(rentalFee)
                .deposit(deposit)
                .total(total)
                .status(PaymentStatus.CONFIRMED)
                .receiptMessage("A digital receipt was sent to " + renterEmail)
                .createdAt(now)
                .updatedAt(now)
                .build();
        }

        payment = paymentRepository.save(payment);

        return toPaymentInfo(payment);
    }

    public PaymentInfo getPayment(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
            .map(this::toPaymentInfo)
            .orElse(null);
    }

    private PaymentInfo toPaymentInfo(Payment payment) {
        PaymentInfo info = new PaymentInfo();
        info.setBookingId(payment.getBookingId());
        info.setRenterEmail(payment.getRenterEmail());
        info.setRentalFee(payment.getRentalFee());
        info.setDeposit(payment.getDeposit());
        info.setTotal(payment.getTotal());
        info.setStatus(payment.getStatus() == PaymentStatus.CONFIRMED 
            ? BookingPaymentStatus.CONFIRMED 
            : BookingPaymentStatus.PENDING);
        info.setReceiptMessage(payment.getReceiptMessage());
        return info;
    }
}
