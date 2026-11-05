package it.somnia.dreamscape.api.user.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import lombok.extern.slf4j.Slf4j;

// Sceglie come mandare il link: email se spring.mail.host è impostato, altrimenti solo nel log
@Slf4j
@Configuration
public class PasswordResetNotifierConfig {

    @Bean
    public PasswordResetNotifier passwordResetNotifier(ObjectProvider<JavaMailSender> mailSender,
            MailProperties mailProperties) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Email non configurata (MAIL_HOST): i link di recupero password finiscono solo nel log");
            return (email, username, link) -> log.info("Link di recupero password per {}: {}", username, link);
        }
        return (email, username, link) -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.from());
            message.setTo(email);
            message.setSubject("Dreamscape: scegli una nuova password");
            message.setText("Ciao " + username + ",\n\n"
                    + "per scegliere una nuova password apri questo link entro un'ora:\n" + link + "\n\n"
                    + "Se non l'hai chiesto tu, ignora questa email: la tua password non cambia.\n\n"
                    + "Il team di Dreamscape");
            try {
                sender.send(message);
            } catch (MailException exception) {
                // La risposta resta 202: chi chiede il link non deve sapere se l'email esiste
                log.error("Email di recupero password non inviata all'utente {}", username, exception);
            }
        };
    }
}
