package it.somnia.dreamscape.api.user.service;

/**
 * Pubblicato quando un utente chiede il link per una nuova password. L'email parte solo dopo il
 * commit e in un altro thread ({@link PasswordResetMailListener}): così la risposta ha gli stessi
 * tempi per un'email registrata e per una che non esiste, e il link arriva solo se il token è salvato.
 */
public record PasswordResetRequestedEvent(String email, String username, String link) {
}
