package com.reservacinema.config;

import com.reservacinema.user.entity.Role;
import com.reservacinema.user.entity.User;
import com.reservacinema.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Cria o primeiro ADMIN quando a aplicação sobe, se ele ainda não existir.
 * Sem isso, ninguém conseguiria promover ninguém (só um admin promove outro admin).
 * ApplicationRunner = código que o Spring executa logo depois de subir a aplicação.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.name}") String name,
                       @Value("${app.admin.email}") String email,
                       @Value("${app.admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(email)) {
            return; // já existe: não fazemos nada (idempotente, pode rodar toda vez que subir)
        }
        userRepository.save(new User(name, email, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Admin inicial criado: {}", email);
    }
}
