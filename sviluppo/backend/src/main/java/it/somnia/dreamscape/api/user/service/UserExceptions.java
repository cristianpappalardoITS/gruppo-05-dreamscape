package it.somnia.dreamscape.api.user.service;

import java.util.List;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;
import it.somnia.dreamscape.api.common.exception.FieldErrorResponse;

/**
 * Errori di registrazione, accesso, profilo e crediti, con i codici della guida per il frontend.
 */
public final class UserExceptions {

    private UserExceptions() {
    }

    public static class UsernameTakenException extends ApiException {
        public UsernameTakenException() {
            super(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Questo username è già usato.",
                    List.of(new FieldErrorResponse("username", "Questo username è già usato.")));
        }
    }

    public static class EmailTakenException extends ApiException {
        public EmailTakenException() {
            super(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Questa email è già usata.",
                    List.of(new FieldErrorResponse("email", "Questa email è già usata.")));
        }
    }

    // Stesso messaggio se manca l'utente o la password è sbagliata: non si rivela chi è registrato
    public static class InvalidCredentialsException extends ApiException {
        public InvalidCredentialsException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Username, email o password non corretti.");
        }
    }

    public static class InvalidRefreshTokenException extends ApiException {
        public InvalidRefreshTokenException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Accesso scaduto. Accedi di nuovo.");
        }
    }

    // Token di accesso ancora valido, ma l'account è stato cancellato o disattivato
    public static class AccountDisabledException extends ApiException {
        public AccountDisabledException() {
            super(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", "Questo account non è più attivo.");
        }
    }

    public static class WrongPasswordException extends ApiException {
        public WrongPasswordException(String field) {
            super(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "La password non è corretta.",
                    List.of(new FieldErrorResponse(field, "La password non è corretta.")));
        }
    }

    public static class InvalidResetTokenException extends ApiException {
        public InvalidResetTokenException() {
            super(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN",
                    "Il link non è più valido. Chiedi un nuovo link per cambiare la password.");
        }
    }

    public static class NotEnoughCreditsException extends ApiException {
        public NotEnoughCreditsException() {
            super(HttpStatus.CONFLICT, "NOT_ENOUGH_CREDITS", "Non hai abbastanza Dream Credits.");
        }
    }
}
