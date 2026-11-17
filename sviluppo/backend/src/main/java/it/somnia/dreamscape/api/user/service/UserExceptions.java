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

    // Account senza password (registrato con Google): la password si sceglie con "Password dimenticata"
    public static class PasswordNotSetException extends ApiException {
        public PasswordNotSetException(String field) {
            super(HttpStatus.BAD_REQUEST, "PASSWORD_NOT_SET",
                    "Il tuo account non ha ancora una password: sceglila con \"Password dimenticata\".",
                    List.of(new FieldErrorResponse(field, "Il tuo account non ha ancora una password.")));
        }
    }

    // Accesso con Google spento: manca GOOGLE_CLIENT_ID
    public static class GoogleLoginDisabledException extends ApiException {
        public GoogleLoginDisabledException() {
            super(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_LOGIN_DISABLED",
                    "L'accesso con Google non è attivo. Accedi con email e password.");
        }
    }

    // Id token di Google falso, scaduto o per un altro sito
    public static class InvalidGoogleTokenException extends ApiException {
        public InvalidGoogleTokenException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Accesso con Google non riuscito. Riprova.");
        }
    }

    // Senza email verificata non si crea un account e non se ne collega uno esistente
    public static class GoogleEmailNotVerifiedException extends ApiException {
        public GoogleEmailNotVerifiedException() {
            super(HttpStatus.FORBIDDEN, "GOOGLE_EMAIL_NOT_VERIFIED",
                    "L'email del tuo account Google non è verificata. Verificala su Google e riprova.");
        }
    }

    // L'account Dreamscape con questa email è già collegato a un altro account Google
    public static class GoogleAccountConflictException extends ApiException {
        public GoogleAccountConflictException() {
            super(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_CONFLICT",
                    "Questo account è già collegato a un altro account Google. Accedi con email e password.");
        }
    }

    // Registrazione con Google: username scelto ma consensi mancanti
    public static class ConsentsRequiredException extends ApiException {
        public ConsentsRequiredException(List<FieldErrorResponse> missing) {
            super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", missing.getFirst().message(), missing);
        }
    }

    public static class NotEnoughCreditsException extends ApiException {
        public NotEnoughCreditsException() {
            super(HttpStatus.CONFLICT, "NOT_ENOUGH_CREDITS", "Non hai abbastanza Dream Credits.");
        }
    }
}
