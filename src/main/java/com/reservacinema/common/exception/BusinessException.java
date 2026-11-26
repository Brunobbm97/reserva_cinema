package com.reservacinema.common.exception;

import org.springframework.http.HttpStatus;

/** Regra de negócio violada (400) */
public class BusinessException extends ApiException {

    public BusinessException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
