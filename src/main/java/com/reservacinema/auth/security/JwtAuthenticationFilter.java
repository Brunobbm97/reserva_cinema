package com.reservacinema.auth.security;

import com.reservacinema.auth.service.JwtService;
import com.reservacinema.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Roda uma vez em TODA requisição, antes dos controllers.
 * Se houver "Authorization: Bearer <token>", valida o token e diz ao Spring Security
 * quem é o usuário. Se não houver token, apenas segue: quem decide se a rota exige
 * login é o SecurityConfig.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtService jwtService, SecurityErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser user = jwtService.parseAccessToken(header.substring(PREFIX.length()));
            // O Spring Security espera os papéis com o prefixo "ROLE_" (ex.: ROLE_ADMIN).
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
            var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
            // A partir daqui, para o resto desta requisição, o usuário está "logado".
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UnauthorizedException e) {
            // Token enviado mas inválido/expirado: respondemos 401 na hora.
            // O cliente deve então usar o refresh token para pegar um novo.
            errorWriter.write(response, 401, "Unauthorized", e.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }
}
