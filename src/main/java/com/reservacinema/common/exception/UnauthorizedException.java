package com.reservacinema.common.exception;

import org.springframework.http.HttpStatus;

/** Credenciais ou token inválidos (401) */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
