package it.somnia.dreamscape.api.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;

// I dati del proprio profilo (GET /me)
public record ProfileResponse(UUID id, String username, String email, UserRole role, Instant createdAt) {

    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getCreatedAt());
    }
}
