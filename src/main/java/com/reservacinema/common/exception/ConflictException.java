package com.reservacinema.common.exception;

import org.springframework.http.HttpStatus;

/** Conflito com o estado atual, ex.: e-mail já cadastrado (409) */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
