package com.reservacinema.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lê as propriedades "app.jwt.*" do application.properties para dentro de um objeto Java.
 * Ex.: app.jwt.access-token-minutes=15 vira accessTokenMinutes = 15.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long accessTokenMinutes, long refreshTokenDays) {
}
