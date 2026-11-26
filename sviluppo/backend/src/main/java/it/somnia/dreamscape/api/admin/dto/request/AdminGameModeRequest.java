package it.somnia.dreamscape.api.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// I valori di una modalità dal backoffice (PUT /admin/game-modes/{code}). Valgono per le partite giocate da adesso:
// quelle già giocate hanno la loro copia in matches
public record AdminGameModeRequest(
        @Schema(example = "Base")
        @NotBlank(message = "Scrivi il nome della modalità.")
        @Size(max = 60, message = "Il nome può avere al massimo 60 caratteri.") String name,

        @Schema(example = "Le regole della Base con gli effetti delle carte.")
        @Size(max = 255, message = "La descrizione può avere al massimo 255 caratteri.") String description,

        @Schema(example = "20")
        @NotNull(message = "Scrivi i punti vita iniziali.")
        @Min(value = 1, message = "I punti vita iniziali sono almeno 1.")
        @Max(value = 1000, message = "I punti vita iniziali possono essere al massimo 1000.")
        Integer startingLifePoints,

        @Schema(example = "60")
        @NotNull(message = "Scrivi il limite di round.")
        @Min(value = 1, message = "Il limite di round è almeno 1.")
        @Max(value = 1000, message = "Il limite di round può essere al massimo 1000.") Integer roundLimit) {
}
