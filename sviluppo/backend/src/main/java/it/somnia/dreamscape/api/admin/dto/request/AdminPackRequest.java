package it.somnia.dreamscape.api.admin.dto.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import it.somnia.dreamscape.api.pack.model.PackType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Pacchetto nuovo o modificato dal backoffice (POST /admin/packs, PUT /admin/packs/{packId}).
// Gli slot sostituiscono quelli di prima; le copertine con id si modificano, quelle senza id si aggiungono,
// quelle che non ci sono più si spengono. Le regole che dipendono dal database le controlla il service.
public record AdminPackRequest(
        @Schema(example = "Sogno Breve")
        @NotBlank(message = "Scrivi il nome del pacchetto.")
        @Size(max = 60, message = "Il nome può avere al massimo 60 caratteri.") String name,

        @Schema(example = "Un pacchetto di prova.")
        @Size(max = 1000, message = "La descrizione può avere al massimo 1000 caratteri.") String description,

        @Schema(description = "SHOP si compra nel negozio; STARTER e WELCOME si regalano nella pagina Benvenuto")
        @NotNull(message = "Scegli il tipo: SHOP, STARTER o WELCOME.") PackType packType,

        @Schema(description = "In Dream Credits: maggiore di 0 per SHOP, 0 per STARTER e WELCOME", example = "250")
        @Min(value = 0, message = "Il prezzo non può essere negativo.") int priceCredits,

        @Schema(description = "Facoltativo: solo carte di questa espansione")
        String cardSetCode,

        @Schema(description = "Facoltativo: solo carte di questa fazione")
        String factionCode,

        @Schema(description = "Percentuale delle Leggendarie che escono in full art, da 0 a 100", example = "10")
        @NotNull(message = "Scrivi la probabilità della full art.")
        @DecimalMin(value = "0", message = "La probabilità della full art va da 0 a 100.")
        @DecimalMax(value = "100", message = "La probabilità della full art va da 0 a 100.") BigDecimal fullArtChance,

        @Schema(description = "Facoltativo, in UTC: inizio della vendita")
        Instant availableFrom,

        @Schema(description = "Facoltativo, in UTC: fine della vendita, dopo l'inizio")
        Instant availableUntil,

        @Schema(description = "Immagine del pacchetto, da POST /admin/images")
        @Size(max = 500) String imageUrl,

        @Schema(description = "false per spegnerlo")
        Boolean isActive,

        @Schema(description = "Almeno uno; con PUT sostituiscono quelli di prima")
        @NotEmpty(message = "Il pacchetto ha almeno uno slot.")
        @Size(max = 10, message = "Al massimo 10 slot.") List<@NotNull @Valid Slot> slots,

        @Schema(description = "Con id si modificano, senza id si aggiungono, quelle che mancano si spengono")
        @Size(max = 10, message = "Al massimo 10 copertine.") List<@NotNull @Valid Cover> covers) {

    /**
     * Uno slot: quante carte dà e con quali rarità. I pesi dello slot sommano a 100 (percentuali).
     *
     * @param cardCount quante carte dà lo slot
     * @param rarities  le rarità possibili con il loro peso
     */
    public record Slot(
            @Schema(example = "4")
            @Min(value = 1, message = "Uno slot dà almeno una carta.")
            @Max(value = 20, message = "Uno slot dà al massimo 20 carte.") int cardCount,
            @NotEmpty(message = "Lo slot ha almeno una rarità.") List<@NotNull @Valid SlotRarity> rarities) {
    }

    /**
     * Una rarità di uno slot con il suo peso.
     *
     * @param rarityCode il codice della rarità, es. RARE
     * @param weight     il peso, da 1 a 100
     */
    public record SlotRarity(
            @Schema(example = "COMMON")
            @NotBlank(message = "Scegli la rarità.") String rarityCode,
            @Schema(description = "Percentuale: i pesi dello slot sommano a 100", example = "100")
            @Min(value = 1, message = "Il peso è almeno 1.") @Max(value = 100, message = "Il peso è al massimo 100.")
            int weight) {
    }

    /**
     * Una copertina del pacchetto.
     *
     * @param id          l'id della copertina da modificare; {@code null} per una copertina nuova
     * @param name        il nome, unico nel pacchetto
     * @param factionCode la fazione del Lord in copertina; può essere {@code null}
     * @param imageUrl    l'immagine, da {@code POST /admin/images} con il tipo COVER
     * @param isActive    {@code false} per spegnerla
     */
    public record Cover(
            UUID id,
            @NotBlank(message = "Scrivi il nome della copertina.") @Size(max = 60) String name,
            String factionCode,
            @NotBlank(message = "Carica l'immagine della copertina.") @Size(max = 500) String imageUrl,
            Boolean isActive) {
    }
}
