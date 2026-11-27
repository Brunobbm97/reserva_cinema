package com.reservacinema.common.exception;

import org.springframework.http.HttpStatus;

/** Pagamento recusado. 402 Payment Required descreve exatamente esta situação. */
public class PaymentRequiredException extends ApiException {

    public PaymentRequiredException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
