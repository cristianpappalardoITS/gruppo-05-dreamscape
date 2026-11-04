package it.somnia.dreamscape.api.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Modifica del profilo (PUT /me): lo username non si cambia
public record UpdateProfileRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email) {
}
