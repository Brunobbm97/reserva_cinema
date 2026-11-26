package com.reservacinema.config;

import com.reservacinema.auth.security.JwtAuthenticationFilter;
import com.reservacinema.auth.security.SecurityErrorWriter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de segurança da API: quais rotas são públicas, quais exigem login,
 * quais exigem ADMIN, e onde entra o nosso filtro de JWT.
 */
@Configuration
@EnableMethodSecurity // habilita @PreAuthorize nos controllers
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   SecurityErrorWriter errorWriter) throws Exception {
        return http
                // CSRF protege formulários com cookie de sessão. Nossa API usa token no header,
                // então essa proteção não se aplica.
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: o servidor não guarda sessão; cada requisição traz seu próprio token.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/showtimes/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                // Sem token numa rota protegida -> 401. Com token mas sem permissão -> 403.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                errorWriter.write(res, 401, "Unauthorized", "Autenticação necessária"))
                        .accessDeniedHandler((req, res, ex) ->
                                errorWriter.write(res, 403, "Forbidden", "Você não tem permissão para esta ação")))
                // Nosso filtro roda antes do filtro padrão de login por formulário do Spring.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Como o filtro é um @Component, o Spring Boot também o registraria sozinho na cadeia
     * geral de filtros, fazendo-o rodar fora do Spring Security. Desligamos esse registro
     * automático: ele deve rodar só dentro da cadeia de segurança configurada acima.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableAutoRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** BCrypt: hash lento e com "sal" aleatório, feito para senhas. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
