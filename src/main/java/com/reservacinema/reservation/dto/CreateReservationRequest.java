package com.reservacinema.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Pedido de reserva. O usuário NÃO vem aqui: ele é lido do token (evita reservar em nome de
 * outra pessoa). seatIds são os assentos escolhidos no mapa da sessão.
 */
public record CreateReservationRequest(
        @NotNull Long showtimeId,
        @NotEmpty List<@NotNull Long> seatIds) {
}
