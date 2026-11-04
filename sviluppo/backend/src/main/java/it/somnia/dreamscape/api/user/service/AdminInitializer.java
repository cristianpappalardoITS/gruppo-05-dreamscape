package it.somnia.dreamscape.api.user.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// All'avvio crea il primo admin da ADMIN_USERNAME, ADMIN_EMAIL e ADMIN_PASSWORD, se non esiste ancora
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminProperties.isConfigured()) {
            if (!userRepository.existsByRole(UserRole.ADMIN)) {
                log.warn("Nessun admin: impostare ADMIN_USERNAME, ADMIN_EMAIL e ADMIN_PASSWORD per crearlo");
            }
            return;
        }
        String username = adminProperties.username().trim();
        String email = adminProperties.email().trim();
        if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        userRepository.save(new User(username, email, passwordEncoder.encode(adminProperties.password()),
                UserRole.ADMIN));
        log.info("Creato l'admin {}", username);
    }
}
