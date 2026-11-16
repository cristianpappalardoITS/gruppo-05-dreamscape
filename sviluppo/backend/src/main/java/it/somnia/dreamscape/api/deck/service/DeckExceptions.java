package it.somnia.dreamscape.api.deck.service;

import java.util.List;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;
import it.somnia.dreamscape.api.common.exception.FieldErrorResponse;

// Errori previsti di collezione e mazzi, con lo stato HTTP e il codice della guida per il frontend
public final class DeckExceptions {

    private DeckExceptions() {
    }

    // Anche per il mazzo di un altro utente: non si dice che esiste
    public static class DeckNotFoundException extends ApiException {
        public DeckNotFoundException() {
            super(HttpStatus.NOT_FOUND, "DECK_NOT_FOUND", "Mazzo non trovato.");
        }
    }

    public static class DeckNameTakenException extends ApiException {
        public DeckNameTakenException() {
            super(HttpStatus.CONFLICT, "DECK_NAME_TAKEN", "Hai già un mazzo con questo nome.",
                    List.of(new FieldErrorResponse("name", "Hai già un mazzo con questo nome.")));
        }
    }

    // Si salvano solo mazzi validi (decisione 017): message è il primo problema, problems li elenca tutti
    public static class InvalidDeckException extends ApiException {

        private final transient List<String> problems;

        public InvalidDeckException(List<String> problems) {
            super(HttpStatus.BAD_REQUEST, "INVALID_DECK", problems.getFirst());
            this.problems = List.copyOf(problems);
        }

        @Override
        public List<String> getProblems() {
            return problems;
        }
    }
}
