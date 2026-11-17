package it.somnia.dreamscape.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Modifica del profilo (PUT /me): lo username non si cambia
public record UpdateProfileRequest(
        @Schema(description = "La nuova email", example = "nuova@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email) {
}
