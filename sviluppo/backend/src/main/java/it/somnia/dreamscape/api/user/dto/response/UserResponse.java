package it.somnia.dreamscape.api.user.dto.response;

import java.util.UUID;

import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;

// L'utente collegato, come arriva dopo l'accesso: con ADMIN il frontend mostra il backoffice
public record UserResponse(UUID id, String username, UserRole role, int credits) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCredits());
    }
}
