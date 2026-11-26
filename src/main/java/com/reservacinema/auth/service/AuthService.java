package com.reservacinema.auth.service;

import com.reservacinema.auth.dto.LoginRequest;
import com.reservacinema.auth.dto.RegisterRequest;
import com.reservacinema.auth.dto.TokenResponse;

/** Cadastro, login, renovação de tokens e logout. */
public interface AuthService {

    /** Cria uma conta com papel USER e já devolve os tokens (usuário sai logado). */
    TokenResponse register(RegisterRequest request);

    /** Confere e-mail e senha e devolve um novo par de tokens. */
    TokenResponse login(LoginRequest request);

    /** Troca um refresh token válido por um novo par (o antigo deixa de valer). */
    TokenResponse refresh(String refreshToken);

    /** Revoga o refresh token, encerrando aquela sessão. */
    void logout(String refreshToken);
}
