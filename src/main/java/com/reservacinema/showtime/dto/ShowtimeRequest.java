package com.reservacinema.showtime.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Dados para agendar uma sessão.
 * startsAt deve vir no formato ISO-8601 com fuso, ex.: "2026-12-01T20:00:00Z".
 * O horário de término é calculado a partir da duração do filme, não é enviado.
 */
public record ShowtimeRequest(
        @NotNull Long movieId,
        @NotNull Long roomId,
        @NotNull @Future Instant startsAt,
        @NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal price) {
}
