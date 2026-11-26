package com.reservacinema.auth.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Escreve erros de segurança (401/403) no mesmo formato JSON do GlobalExceptionHandler.
 * É necessário porque esses erros acontecem nos filtros, ANTES de chegar nos controllers,
 * onde o @RestControllerAdvice não alcança.
 */
@Component
public class SecurityErrorWriter {

    public void write(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","details":[]}"""
                .formatted(Instant.now(), status, error, message);
        response.getWriter().write(json);
    }
}
