package it.somnia.dreamscape.api.card.service;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;

// Errori previsti del catalogo, con lo stato HTTP e il codice della guida per il frontend
public final class CardExceptions {

    private CardExceptions() {
    }

    public static class CardNotFoundException extends ApiException {
        public CardNotFoundException() {
            super(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Carta non trovata.");
        }
    }
}
