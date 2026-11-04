package it.somnia.dreamscape.api.user.dto.response;

import java.util.UUID;

import it.somnia.dreamscape.api.user.model.User;

// Un altro giocatore in liste, amici, scambi e partite: solo id e username, mai email o crediti
public record UserSummaryResponse(UUID id, String username) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getUsername());
    }
}
