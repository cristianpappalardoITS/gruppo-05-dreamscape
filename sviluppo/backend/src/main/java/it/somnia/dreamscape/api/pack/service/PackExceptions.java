package it.somnia.dreamscape.api.pack.service;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;

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
}
