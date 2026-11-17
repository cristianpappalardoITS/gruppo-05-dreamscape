package it.somnia.dreamscape.api.friend.service;

import java.util.List;

import org.springframework.http.HttpStatus;

import it.somnia.dreamscape.api.common.exception.ApiException;
import it.somnia.dreamscape.api.common.exception.FieldErrorResponse;

/**
 * Errori della lista amici, con i codici della guida per il frontend. Quelli della richiesta di amicizia
 * hanno anche l'errore sul campo {@code username}, da mostrare accanto alla casella di ricerca.
 */
public final class FriendExceptions {

    private FriendExceptions() {
    }

    // Nessun giocatore attivo con quello username o id (anche account cancellati e admin)
    public static class UserNotFoundException extends ApiException {
        public UserNotFoundException() {
            super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Nessun Dreamweaver con questo username.");
        }

        public UserNotFoundException(String field) {
            super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Nessun Dreamweaver con questo username.",
                    List.of(new FieldErrorResponse(field, "Nessun Dreamweaver con questo username.")));
        }
    }

    public static class CannotFriendYourselfException extends ApiException {
        public CannotFriendYourselfException() {
            super(HttpStatus.BAD_REQUEST, "CANNOT_FRIEND_YOURSELF", "Non puoi chiedere l'amicizia a te stesso.",
                    List.of(new FieldErrorResponse("username", "Non puoi chiedere l'amicizia a te stesso.")));
        }
    }

    public static class AlreadyFriendsException extends ApiException {
        public AlreadyFriendsException() {
            super(HttpStatus.CONFLICT, "ALREADY_FRIENDS", "Siete già amici.",
                    List.of(new FieldErrorResponse("username", "Siete già amici.")));
        }
    }

    public static class FriendRequestAlreadySentException extends ApiException {
        public FriendRequestAlreadySentException() {
            super(HttpStatus.CONFLICT, "FRIEND_REQUEST_ALREADY_SENT", "Hai già mandato una richiesta a questo Dreamweaver.",
                    List.of(new FieldErrorResponse("username", "Hai già mandato una richiesta a questo Dreamweaver.")));
        }
    }

    // Richiesta che non esiste, già accettata, o di cui l'utente non è il destinatario (accettazione)
    public static class FriendRequestNotFoundException extends ApiException {
        public FriendRequestNotFoundException() {
            super(HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND", "Richiesta di amicizia non trovata.");
        }
    }

    // DELETE /friends/{userId} verso chi non è un amico
    public static class FriendNotFoundException extends ApiException {
        public FriendNotFoundException() {
            super(HttpStatus.NOT_FOUND, "FRIEND_NOT_FOUND", "Questo Dreamweaver non è tra i tuoi amici.");
        }
    }

    // Collezione di un altro giocatore o scambio con chi non è un amico
    public static class NotFriendsException extends ApiException {
        public NotFriendsException() {
            super(HttpStatus.FORBIDDEN, "NOT_FRIENDS", "Puoi farlo solo con i tuoi amici.");
        }
    }
}
