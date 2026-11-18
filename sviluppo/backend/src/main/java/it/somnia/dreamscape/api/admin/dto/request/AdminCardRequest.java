package it.somnia.dreamscape.api.admin.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Carta nuova o modificata dal backoffice (POST /admin/cards, PUT /admin/cards/{cardId}).
// Carta normale: name, attack, defense, factionCode, rarityCode, cardSetCode obbligatori.
// Full art: solo code (codice della carta base + "-FA"), baseCardId e l'illustrazione (decisione 025).
// Le regole che dipendono dal database (codici esistenti, valore X, Leggendaria) le controlla il service.
public record AdminCardRequest(
        @Schema(description = "Lettere maiuscole, numeri e trattini. Full art: il codice della carta base + -FA. "
                + "Non si cambia", example = "BASE-013")
        @NotBlank(message = "Scrivi il codice della carta.")
        @Size(max = 20, message = "Il codice può avere al massimo 20 caratteri.")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "Il codice ha solo lettere maiuscole, numeri e trattini.")
        String code,

        @Schema(description = "Solo per la full art: l'id della carta Leggendaria. Non si cambia")
        UUID baseCardId,

        @Schema(description = "Obbligatorio per una carta normale, vuoto per la full art", example = "Il Sogno Lucido")
        @Size(max = 80, message = "Il nome può avere al massimo 80 caratteri.") String name,

        @Schema(example = "Sai di sognare, e il sogno ti obbedisce.")
        @Size(max = 1000, message = "La descrizione può avere al massimo 1000 caratteri.") String description,

        @Schema(description = "Da 1 a 99", example = "10")
        @Min(value = 1, message = "L'attacco è almeno 1.") @Max(value = 99, message = "L'attacco è al massimo 99.")
        Integer attack,

        @Schema(description = "Da 1 a 99", example = "14")
        @Min(value = 1, message = "La difesa è almeno 1.") @Max(value = 99, message = "La difesa è al massimo 99.")
        Integer defense,

        @Schema(example = "DREAMS")
        String factionCode,

        @Schema(example = "LEGENDARY")
        String rarityCode,

        @Schema(description = "L'espansione", example = "BASE")
        String cardSetCode,

        @Schema(description = "Facoltativo", example = "REST")
        String effectCode,

        @Schema(description = "Il valore X, da 1 a 20: solo per gli effetti che lo hanno", example = "3")
        @Min(value = 1, message = "Il valore X è almeno 1.") @Max(value = 20, message = "Il valore X è al massimo 20.")
        Integer effectValue,

        @Schema(description = "url ricevuto da POST /admin/images. Nella modifica, senza le due URL l'illustrazione "
                + "resta quella di prima; con le due URL vuote (\"\") si toglie",
                example = "/images/cards/base-013-v1.webp")
        @Size(max = 500) String illustrationUrl,

        @Schema(description = "thumbUrl ricevuto da POST /admin/images: sempre insieme a illustrationUrl",
                example = "/images/cards/base-013-v1-thumb.webp")
        @Size(max = 500) String illustrationThumbUrl,

        @Schema(description = "Il punto del ritaglio in verticale, da 0 (in alto) a 1; di base 0.40. Nella modifica, "
                + "se manca resta quello di prima", example = "0.40")
        @DecimalMin(value = "0", message = "Il ritaglio va da 0 a 1.")
        @DecimalMax(value = "1", message = "Il ritaglio va da 0 a 1.") BigDecimal illustrationFocusY,

        @Schema(description = "false per disattivarla; di base true")
        Boolean isActive) {
}
