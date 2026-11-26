package com.reservacinema.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Formato padrão de erro devolvido pela API.
 * "record" é uma classe imutável e enxuta do Java: o compilador gera construtor,
 * getters, equals e toString a partir dos campos.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, List.of());
    }
}
