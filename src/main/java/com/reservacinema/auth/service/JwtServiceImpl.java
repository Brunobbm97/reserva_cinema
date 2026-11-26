package com.reservacinema.auth.service;

import com.reservacinema.auth.security.AuthenticatedUser;
import com.reservacinema.auth.security.JwtProperties;
import com.reservacinema.common.exception.UnauthorizedException;
import com.reservacinema.user.entity.Role;
import com.reservacinema.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtServiceImpl(JwtProperties properties) {
        // HMAC-SHA256: a mesma chave assina e confere. Só o servidor conhece a chave,
        // então ninguém consegue alterar o conteúdo do token sem invalidar a assinatura.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenTtl = Duration.ofMinutes(properties.accessTokenMinutes());
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())          // "sub": de quem é o token
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())      // usado para autorização (ADMIN/USER)
                .issuedAt(Date.from(now))                  // "iat": quando foi emitido
                .expiration(Date.from(now.plus(accessTokenTtl))) // "exp": quando expira
                .signWith(key)
                .compact();
    }

    @Override
    public AuthenticatedUser parseAccessToken(String token) {
        try {
            // parseSignedClaims confere a assinatura E a expiração; se algo falhar, lança JwtException.
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Token inválido ou expirado");
        }
    }

    @Override
    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
