-- =========================================================
-- V3: pagamentos (simulados)
-- =========================================================
-- Cada tentativa de pagamento de uma reserva vira uma linha aqui.
-- O pagamento é SIMULADO: não há dinheiro real nem gateway externo.
CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT         NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    amount         NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    status         VARCHAR(20)    NOT NULL CHECK (status IN ('APPROVED', 'DECLINED')),
    method         VARCHAR(20)    NOT NULL,
    -- "Comprovante" fake: um código que identifica a transação aprovada.
    transaction_ref VARCHAR(40),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX ix_payments_reservation ON payments (reservation_id);
