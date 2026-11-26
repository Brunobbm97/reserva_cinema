package com.reservacinema.auth.service;

import com.reservacinema.auth.security.AuthenticatedUser;
import com.reservacinema.user.entity.User;

/** Cria e lê access tokens (JWT). */
public interface JwtService {

    /** Gera um JWT assinado contendo id, e-mail e papel do usuário. */
    String generateAccessToken(User user);

    /**
     * Valida assinatura e validade do token e devolve o usuário que está nele.
     * Lança UnauthorizedException se o token for inválido ou estiver expirado.
     */
    AuthenticatedUser parseAccessToken(String token);

    /** Tempo de vida do access token, em segundos (vai na resposta do login). */
    long accessTokenTtlSeconds();
}
