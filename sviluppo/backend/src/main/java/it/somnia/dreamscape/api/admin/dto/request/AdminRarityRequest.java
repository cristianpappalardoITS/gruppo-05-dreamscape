package it.somnia.dreamscape.api.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// I limiti di una rarità nei mazzi, dal backoffice (PUT /admin/rarities/{code}). Codice e ordine non cambiano
public record AdminRarityRequest(
        @Schema(example = "Rara")
        @NotBlank(message = "Scrivi il nome della rarità.")
        @Size(max = 50, message = "Il nome può avere al massimo 50 caratteri.") String name,

        @Schema(description = "Quante carte di questa rarità al massimo in un mazzo; null = nessun limite",
                example = "8", nullable = true)
        @Min(value = 0, message = "Il limite non può essere negativo.")
        @Max(value = 100, message = "Il limite può essere al massimo 100.") Integer maxPerDeck,

        @Schema(description = "Quante copie della stessa carta al massimo in un mazzo", example = "2")
        @NotNull(message = "Scrivi quante copie della stessa carta si possono mettere.")
        @Min(value = 1, message = "Si può mettere almeno una copia.")
        @Max(value = 20, message = "Le copie possono essere al massimo 20.") Integer maxCopies) {
}
