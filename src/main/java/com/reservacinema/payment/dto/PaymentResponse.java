package com.reservacinema.payment.dto;

import com.reservacinema.payment.entity.Payment;
import com.reservacinema.payment.entity.PaymentStatus;
import com.reservacinema.reservation.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long reservationId,
        BigDecimal amount,
        PaymentStatus status,
        String method,
        String transactionRef,
        ReservationStatus reservationStatus,
        Instant createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getReservation().getId(),
                p.getAmount(),
                p.getStatus(),
                p.getMethod(),
                p.getTransactionRef(),
                p.getReservation().getStatus(),
                p.getCreatedAt());
    }
}
