package com.reservacinema.reservation.entity;

/**
 * Ciclo de vida de uma reserva:
 * PENDING_PAYMENT -> CONFIRMED (pagamento aprovado)
 * PENDING_PAYMENT ou CONFIRMED -> CANCELLED (usuário cancelou antes da sessão)
 */
public enum ReservationStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED
}
