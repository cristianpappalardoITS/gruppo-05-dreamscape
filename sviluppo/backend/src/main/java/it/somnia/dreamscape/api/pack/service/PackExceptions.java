package it.somnia.dreamscape.api.pack.service;

import java.util.List;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;
import it.somnia.dreamscape.api.common.exception.FieldErrorResponse;

// Errori previsti di negozio e aperture, con lo stato HTTP e il codice della guida per il frontend
public final class PackExceptions {

    private PackExceptions() {
    }

    // Anche per l'apertura di un altro utente: non si dice che esiste
    public static class PackOpeningNotFoundException extends ApiException {
        public PackOpeningNotFoundException() {
            super(HttpStatus.NOT_FOUND, "PACK_OPENING_NOT_FOUND", "Apertura non trovata.");
        }
    }

    // Pacchetto che non esiste o che non si compra (set iniziale, benvenuto)
    public static class PackNotFoundException extends ApiException {
        public PackNotFoundException() {
            super(HttpStatus.NOT_FOUND, "PACK_NOT_FOUND", "Pacchetto non trovato.");
        }
    }

    // Pacchetto del negozio spento o fuori dal periodo di vendita
    public static class PackNotOnSaleException extends ApiException {
        public PackNotOnSaleException() {
            super(HttpStatus.CONFLICT, "PACK_NOT_ON_SALE", "Questo pacchetto non è più in vendita.");
        }
    }

    // Copertina di un altro pacchetto o non più attiva
    public static class InvalidCoverException extends ApiException {
        public InvalidCoverException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_COVER", "Copertina non valida per questo pacchetto.",
                    List.of(new FieldErrorResponse("coverId", "Scegli una delle copertine del pacchetto.")));
        }
    }
}
