package com.reservacinema.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Dados do "cartão" para o pagamento simulado. NÃO é um pagamento real:
 * nenhuma informação sensível deve ser usada aqui.
 *
 * Regra de simulação (ver guia): cartões cujo número termina em "0000" são RECUSADOS,
 * para você conseguir testar o caminho de falha. Qualquer outro número é aprovado.
 */
public record PaymentRequest(
        @NotBlank @Pattern(regexp = "\\d{16}", message = "o número do cartão deve ter 16 dígitos") String cardNumber,
        @NotBlank String cardHolder,
        @NotBlank @Pattern(regexp = "CREDIT|DEBIT|PIX", message = "método deve ser CREDIT, DEBIT ou PIX") String method) {
}
