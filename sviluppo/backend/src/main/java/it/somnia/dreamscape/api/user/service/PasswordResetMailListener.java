package it.somnia.dreamscape.api.user.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

// Manda il link del recupero password dopo il commit, fuori dalla richiesta HTTP
@Component
@RequiredArgsConstructor
public class PasswordResetMailListener {

    private final PasswordResetNotifier notifier;

    @Async
    @TransactionalEventListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        notifier.sendResetLink(event.email(), event.username(), event.link());
    }
}
