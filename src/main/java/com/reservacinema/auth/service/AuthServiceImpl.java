package com.reservacinema.auth.service;

import com.reservacinema.auth.dto.LoginRequest;
import com.reservacinema.auth.dto.RegisterRequest;
import com.reservacinema.auth.dto.TokenResponse;
import com.reservacinema.auth.entity.RefreshToken;
import com.reservacinema.auth.repository.RefreshTokenRepository;
import com.reservacinema.auth.security.JwtProperties;
import com.reservacinema.common.exception.ConflictException;
import com.reservacinema.common.exception.UnauthorizedException;
import com.reservacinema.user.entity.Role;
import com.reservacinema.user.entity.User;
import com.reservacinema.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTokenTtl;

    // Injeção de dependência pelo construtor: o Spring cria este service e entrega
    // os objetos que ele precisa. Não usamos "new" para nenhum deles.
    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenTtl = Duration.ofDays(jwtProperties.refreshTokenDays());
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }
        // Todo cadastro público vira USER. Só um ADMIN pode promover alguém.
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.USER);
        userRepository.save(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                // Mesma mensagem para "e-mail não existe" e "senha errada":
                // assim ninguém descobre quais e-mails estão cadastrados.
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha inválidos"));
        return issueTokens(user);
    }

    @Override
    // noRollbackFor: normalmente uma exceção desfaz a transação. Aqui queremos que a
    // revogação de todas as sessões (token reutilizado) seja gravada mesmo lançando o erro.
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (stored.isRevoked()) {
            // Um token revogado (já usado ou de uma sessão encerrada) apareceu de novo:
            // pode ter sido roubado.
            // Por segurança, derrubamos todas as sessões desse usuário.
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            throw new UnauthorizedException("Refresh token revogado; faça login novamente");
        }
        if (!stored.isUsable()) {
            throw new UnauthorizedException("Refresh token expirado; faça login novamente");
        }

        // Rotação: o refresh token antigo deixa de valer e um novo par é emitido.
        stored.revoke();
        return issueTokens(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        // Se o token não existir, não fazemos nada: logout é sempre "bem-sucedido".
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    /** Gera access token (JWT) + refresh token (aleatório, salvo como hash). */
    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);

        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefreshToken), Instant.now().plus(refreshTokenTtl)));

        return new TokenResponse(accessToken, rawRefreshToken, "Bearer", jwtService.accessTokenTtlSeconds());
    }

    /** SHA-256 em hexadecimal (64 caracteres). */
    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
