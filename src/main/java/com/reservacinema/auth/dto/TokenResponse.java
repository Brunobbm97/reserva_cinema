package com.reservacinema.auth.dto;

/**
 * Resposta do login, cadastro e refresh.
 * accessToken: vai no header "Authorization: Bearer ..." de cada requisição.
 * refreshToken: guardado pelo cliente, usado só para pedir um novo par de tokens.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {
}
