package com.reservacinema.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base da aplicação. Cada exceção "sabe" qual status HTTP ela representa,
 * e o GlobalExceptionHandler transforma isso numa resposta JSON padronizada.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
