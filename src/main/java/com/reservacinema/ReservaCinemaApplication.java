package com.reservacinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Excluímos o usuário/senha padrão que o Spring Security cria sozinho: usamos JWT com os nossos usuários.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan // encontra classes com @ConfigurationProperties (ex.: JwtProperties)
public class ReservaCinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservaCinemaApplication.class, args);
    }
}
