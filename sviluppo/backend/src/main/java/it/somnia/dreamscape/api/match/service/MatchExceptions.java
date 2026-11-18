package it.somnia.dreamscape.api.match.service;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;

/**
 * Errori delle partite, con i codici della guida per il frontend.
 */
public final class MatchExceptions {

    private MatchExceptions() {
    }

    // Motore spento, lento, senza chiave o in errore (decisione 029): la partita non si gioca e non si salva niente
    public static class EngineUnavailableException extends ApiException {
        public EngineUnavailableException() {
            super(HttpStatus.SERVICE_UNAVAILABLE, "ENGINE_UNAVAILABLE", "Partita non disponibile, riprova tra poco.");
        }
    }

    // La partita non esiste o l'utente non l'ha giocata: non si dice quale delle due
    public static class MatchNotFoundException extends ApiException {
        public MatchNotFoundException() {
            super(HttpStatus.NOT_FOUND, "MATCH_NOT_FOUND", "Partita non trovata.");
        }
    }
}
