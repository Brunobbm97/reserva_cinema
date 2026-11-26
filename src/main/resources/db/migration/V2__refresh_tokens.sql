-- =========================================================
-- V2: refresh tokens
-- =========================================================
-- O access token (JWT) não fica salvo no banco: ele se valida sozinho pela assinatura.
-- Já o refresh token fica salvo, para podermos revogá-lo (logout) e trocá-lo a cada uso.
-- Guardamos só o HASH do token: se alguém ler o banco, não consegue usar os tokens.
CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
