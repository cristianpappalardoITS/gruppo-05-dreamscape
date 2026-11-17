package it.somnia.dreamscape.api.trade.service;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;

/**
 * Errori degli scambi, con i codici della guida per il frontend. Il messaggio dice quale carta e quante copie.
 */
public final class TradeExceptions {

    private TradeExceptions() {
    }

    // Scambio che non esiste o di cui l'utente non fa parte
    public static class TradeNotFoundException extends ApiException {
        public TradeNotFoundException() {
            super(HttpStatus.NOT_FOUND, "TRADE_NOT_FOUND", "Scambio non trovato.");
        }
    }

    // Già accettato, rifiutato o annullato
    public static class TradeNotPendingException extends ApiException {
        public TradeNotPendingException() {
            super(HttpStatus.CONFLICT, "TRADE_NOT_PENDING", "Questo scambio è già chiuso.");
        }
    }

    // All'accettazione i due non sono più amici: lo scambio è stato annullato (decisione 026)
    public static class TradeCancelledException extends ApiException {
        public TradeCancelledException() {
            super(HttpStatus.CONFLICT, "TRADE_CANCELLED", "Non siete più amici: lo scambio è stato annullato.");
        }
    }

    // Rifiuta solo chi riceve, annulla solo chi propone
    public static class TradeActionNotAllowedException extends ApiException {
        public TradeActionNotAllowedException() {
            super(HttpStatus.FORBIDDEN, "FORBIDDEN", "Non hai i permessi per questa operazione.");
        }
    }

    // Proposta che non ha senso: a se stessi, stessa carta offerta e richiesta
    public static class InvalidTradeException extends ApiException {
        public InvalidTradeException(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_TRADE", message);
        }
    }

    // Offre più copie di quelle che possiede
    public static class CardsNotOwnedException extends ApiException {
        public CardsNotOwnedException(String message) {
            super(HttpStatus.CONFLICT, "CARDS_NOT_OWNED", message);
        }
    }

    // Le possiede, ma alcune sono già offerte in altri scambi in attesa (decisione 024)
    public static class CardsLockedInTradeException extends ApiException {
        public CardsLockedInTradeException(String message) {
            super(HttpStatus.CONFLICT, "CARDS_LOCKED_IN_TRADE", message);
        }
    }

    // L'amico non ha abbastanza copie disponibili delle carte richieste (alla proposta e all'accettazione)
    public static class RequestedCardsNotAvailableException extends ApiException {
        public RequestedCardsNotAvailableException(String message) {
            super(HttpStatus.CONFLICT, "REQUESTED_CARDS_NOT_AVAILABLE", message);
        }
    }
}
