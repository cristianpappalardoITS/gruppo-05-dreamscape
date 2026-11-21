package it.somnia.dreamscape.api.admin.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Una fazione modificata dal backoffice (PUT /admin/factions/{code}). Codice e carta Lord non cambiano; i vantaggi
// mandati sostituiscono quelli di prima. Le regole che dipendono dal database le controlla il service
public record AdminFactionRequest(
        @Schema(example = "Dreams")
        @NotBlank(message = "Scrivi il nome della fazione.")
        @Size(max = 50, message = "Il nome può avere al massimo 50 caratteri.") String name,

        @Schema(example = "Il regno dei sogni lucidi.")
        @Size(max = 2000, message = "La descrizione può avere al massimo 2000 caratteri.") String description,

        @Schema(example = "Dreams Lord")
        @Size(max = 50, message = "Il titolo del Lord può avere al massimo 50 caratteri.") String lordTitle,

        @Schema(description = "Contro quali fazioni è forte e con quale bonus; lista vuota se non è forte contro "
                + "nessuna")
        @NotNull(message = "Manda i vantaggi della fazione (anche una lista vuota).")
        @Size(max = 10, message = "Al massimo 10 vantaggi.") List<@NotNull @Valid Advantage> advantages) {

    /**
     * Un vantaggio: la creatura di questa fazione ha il bonus contro la creatura di {@code targetFaction}.
     *
     * @param targetFaction il codice della fazione contro cui vale, es. MEMORIES
     * @param attackBonus   punti di attacco in più
     * @param defenseBonus  punti di difesa in più
     */
    public record Advantage(
            @Schema(example = "MEMORIES")
            @NotBlank(message = "Scegli la fazione contro cui vale il vantaggio.") String targetFaction,

            @Schema(example = "1")
            @NotNull(message = "Scrivi il bonus di attacco (0 se non c'è).")
            @Min(value = 0, message = "Il bonus non può essere negativo.")
            @Max(value = 20, message = "Il bonus può essere al massimo 20.") Integer attackBonus,

            @Schema(example = "0")
            @NotNull(message = "Scrivi il bonus di difesa (0 se non c'è).")
            @Min(value = 0, message = "Il bonus non può essere negativo.")
            @Max(value = 20, message = "Il bonus può essere al massimo 20.") Integer defenseBonus) {
    }
}
