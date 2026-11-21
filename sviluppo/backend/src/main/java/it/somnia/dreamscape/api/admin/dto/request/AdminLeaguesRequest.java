package it.somnia.dreamscape.api.admin.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Soglie e premi delle leghe dal backoffice (PUT /admin/leagues): sempre tutte le leghe insieme, così il service
// controlla l'ordine delle soglie in una volta sola. Codici e nomi non si cambiano
public record AdminLeaguesRequest(
        @Schema(description = "Tutte le leghe, una volta ciascuna")
        @NotEmpty(message = "Manda tutte le leghe.")
        @Size(max = 20, message = "Al massimo 20 leghe.") List<@NotNull @Valid League> leagues) {

    /**
     * La soglia e il premio di una lega.
     *
     * @param code          il codice della lega, es. DREAMER
     * @param minRating     il punteggio da cui si entra; 0 per la lega più bassa, poi sempre più alto
     * @param rewardCredits i Dream Credits della prima volta che si entra; 0 = nessun premio
     */
    public record League(
            @Schema(example = "DREAMER")
            @NotBlank(message = "Scrivi il codice della lega.") String code,

            @Schema(example = "1100")
            @NotNull(message = "Scrivi il punteggio da cui si entra nella lega.")
            @Min(value = 0, message = "Il punteggio non può essere negativo.")
            @Max(value = 100000, message = "Il punteggio può essere al massimo 100000.") Integer minRating,

            @Schema(example = "100")
            @NotNull(message = "Scrivi il premio della lega (0 se non c'è).")
            @Min(value = 0, message = "Il premio non può essere negativo.")
            @Max(value = 100000, message = "Il premio può essere al massimo 100000 DC.") Integer rewardCredits) {
    }
}
