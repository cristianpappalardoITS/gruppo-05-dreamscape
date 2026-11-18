package it.somnia.dreamscape.api.challenge.service;

import java.util.List;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;

/**
 * Errori delle sfide tra amici, con i codici della guida per il frontend.
 */
public final class ChallengeExceptions {

    private ChallengeExceptions() {
    }

    // Sfida che non si può fare, es. a se stessi
    public static class InvalidChallengeException extends ApiException {
        public InvalidChallengeException(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_CHALLENGE", message);
        }
    }

    // Al massimo una sfida in attesa per coppia, in tutte e due le direzioni (uq_challenges_pending_pair)
    public static class ChallengeAlreadyPendingException extends ApiException {
        public ChallengeAlreadyPendingException() {
            super(HttpStatus.CONFLICT, "CHALLENGE_ALREADY_PENDING", "C'è già una sfida in attesa tra voi due.");
        }
    }

    // La sfida non esiste o non riguarda l'utente: non si dice quale delle due
    public static class ChallengeNotFoundException extends ApiException {
        public ChallengeNotFoundException() {
            super(HttpStatus.NOT_FOUND, "CHALLENGE_NOT_FOUND", "Sfida non trovata.");
        }
    }

    public static class ChallengeNotPendingException extends ApiException {
        public ChallengeNotPendingException() {
            super(HttpStatus.CONFLICT, "CHALLENGE_NOT_PENDING", "Questa sfida è già chiusa.");
        }
    }

    // Accettare o rifiutare la propria sfida, o annullare quella di un altro
    public static class ChallengeActionNotAllowedException extends ApiException {
        public ChallengeActionNotAllowedException() {
            super(HttpStatus.FORBIDDEN, "FORBIDDEN", "Non hai i permessi per questa operazione.");
        }
    }

    // All'accettazione i due non sono più amici: la sfida si annulla da sola (decisione 026)
    public static class ChallengeCancelledException extends ApiException {
        public ChallengeCancelledException() {
            super(HttpStatus.CONFLICT, "CHALLENGE_CANCELLED", "Non siete più amici: la sfida è stata annullata.");
        }
    }

    // All'accettazione il mazzo di chi ha sfidato non va più bene: niente partita, la sfida resta in attesa
    public static class ChallengerDeckInvalidException extends ApiException {

        private final transient List<String> problems;

        public ChallengerDeckInvalidException(String challengerUsername, List<String> problems) {
            super(HttpStatus.CONFLICT, "CHALLENGER_DECK_INVALID", "Il mazzo di " + challengerUsername
                    + " non è più valido: la sfida resta in attesa finché non lo sistema.");
            this.problems = List.copyOf(problems);
        }

        @Override
        public List<String> getProblems() {
            return problems;
        }
    }
}
