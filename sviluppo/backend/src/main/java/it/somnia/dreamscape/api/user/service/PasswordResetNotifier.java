package it.somnia.dreamscape.api.user.service;

/**
 * Manda all'utente il link per scegliere una nuova password. Con il server email configurato
 * lo manda per email (in sviluppo arriva in Mailpit); senza, lo scrive nel log.
 */
public interface PasswordResetNotifier {

    void sendResetLink(String email, String username, String link);
}
