package com.vaicomtudo.backend.data.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID bookingId;

    @ManyToOne
    @JoinColumn(name = "renter_id")
    private User renter;

    @Column(nullable = false, length = 255)
    private String renterEmail;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal rentalFee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal deposit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(length = 500)
    private String receiptMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING,
        CONFIRMED
    }
}
