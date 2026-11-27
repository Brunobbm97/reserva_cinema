package com.reservacinema.payment.service;

import com.reservacinema.common.exception.PaymentRequiredException;

/** Pagamento recusado pelo "gateway" simulado. Vira HTTP 402. */
public class PaymentDeclinedException extends PaymentRequiredException {

    public PaymentDeclinedException() {
        super("Pagamento recusado. Verifique os dados do cartão e tente novamente");
    }
}
