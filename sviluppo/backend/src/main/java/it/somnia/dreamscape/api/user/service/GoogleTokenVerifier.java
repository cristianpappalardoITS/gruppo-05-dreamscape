package it.somnia.dreamscape.api.user.service;

import it.somnia.dreamscape.api.user.service.UserExceptions.InvalidGoogleTokenException;

/**
 * Controlla l'id token che il pulsante di Google dà al frontend e ne legge chi è la persona (decisione 032).
 *
 * <p>È un'interfaccia perché nei test si sostituisce con una finta: il token vero lo firma solo Google.
 */
public interface GoogleTokenVerifier {

    /**
     * Verifica firma, scadenza, emittente e destinatario dell'id token di Google.
     *
     * @param idToken l'id token ricevuto dal frontend
     * @return l'account Google del token
     * @throws InvalidGoogleTokenException se il token non è valido, è scaduto o non è per il nostro client id
     */
    GoogleIdentity verify(String idToken);

    /**
     * L'account Google letto da un id token valido.
     *
     * @param subject       l'id stabile dell'account Google (claim {@code sub})
     * @param email         l'email dell'account Google
     * @param emailVerified {@code true} se Google ha verificato l'email (claim {@code email_verified})
     */
    record GoogleIdentity(String subject, String email, boolean emailVerified) {
    }
}
