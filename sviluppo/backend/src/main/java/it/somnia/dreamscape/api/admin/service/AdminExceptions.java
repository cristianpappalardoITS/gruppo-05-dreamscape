package it.somnia.dreamscape.api.admin.service;

import java.util.List;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;
import it.somnia.dreamscape.api.common.exception.FieldErrorResponse;

// Errori previsti del backoffice, con lo stato HTTP e il codice della guida per il frontend
public final class AdminExceptions {

    private AdminExceptions() {
    }

    // File che non è un'immagine PNG o JPEG leggibile
    public static class InvalidImageException extends ApiException {
        public InvalidImageException() {
            this("Carica un'immagine PNG o JPEG.");
        }

        public InvalidImageException(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", message, List.of(new FieldErrorResponse("file", message)));
        }
    }

    // Dato del form sbagliato che le annotazioni non possono controllare (es. una fazione che non esiste)
    public static class InvalidAdminFieldException extends ApiException {
        public InvalidAdminFieldException(String field, String message) {
            super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, List.of(new FieldErrorResponse(field, message)));
        }
    }

    public static class CardCodeTakenException extends ApiException {
        public CardCodeTakenException() {
            super(HttpStatus.CONFLICT, "CARD_CODE_TAKEN", "Esiste già una carta con questo codice.",
                    List.of(new FieldErrorResponse("code", "Esiste già una carta con questo codice.")));
        }
    }

    public static class CardNameTakenException extends ApiException {
        public CardNameTakenException() {
            super(HttpStatus.CONFLICT, "CARD_NAME_TAKEN", "Esiste già una carta con questo nome.",
                    List.of(new FieldErrorResponse("name", "Esiste già una carta con questo nome.")));
        }
    }

    // Al massimo una full art per carta (uq_cards_full_art)
    public static class FullArtExistsException extends ApiException {
        public FullArtExistsException() {
            super(HttpStatus.CONFLICT, "FULL_ART_EXISTS", "Questa carta ha già la sua full art.",
                    List.of(new FieldErrorResponse("baseCardId", "Questa carta ha già la sua full art.")));
        }
    }

    public static class PackNameTakenException extends ApiException {
        public PackNameTakenException() {
            super(HttpStatus.CONFLICT, "PACK_NAME_TAKEN", "Esiste già un pacchetto con questo nome.",
                    List.of(new FieldErrorResponse("name", "Esiste già un pacchetto con questo nome.")));
        }
    }
}
