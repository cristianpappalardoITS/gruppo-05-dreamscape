package it.somnia.dreamscape.api.user.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email del portale (prefisso dreamscape.mail).
 *
 * @param from             mittente delle email
 * @param passwordResetUrl indirizzo della pagina "Nuova password", a cui si aggiunge il token
 */
@ConfigurationProperties(prefix = "dreamscape.mail")
public record MailProperties(String from, String passwordResetUrl) {
}
