package it.somnia.dreamscape.api.admin.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

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
        @NotBlank(message = "Scrivi il codice della carta.")
        @Size(max = 20, message = "Il codice può avere al massimo 20 caratteri.")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "Il codice ha solo lettere maiuscole, numeri e trattini.")
        String code,

        UUID baseCardId,

        @Size(max = 80, message = "Il nome può avere al massimo 80 caratteri.") String name,

        @Size(max = 1000, message = "La descrizione può avere al massimo 1000 caratteri.") String description,

        @Min(value = 1, message = "L'attacco è almeno 1.") @Max(value = 99, message = "L'attacco è al massimo 99.")
        Integer attack,

        @Min(value = 1, message = "La difesa è almeno 1.") @Max(value = 99, message = "La difesa è al massimo 99.")
        Integer defense,

        String factionCode,

        String rarityCode,

        String cardSetCode,

        String effectCode,

        @Min(value = 1, message = "Il valore X è almeno 1.") @Max(value = 20, message = "Il valore X è al massimo 20.")
        Integer effectValue,

        @Size(max = 500) String illustrationUrl,

        @Size(max = 500) String illustrationThumbUrl,

        @DecimalMin(value = "0", message = "Il ritaglio va da 0 a 1.")
        @DecimalMax(value = "1", message = "Il ritaglio va da 0 a 1.") BigDecimal illustrationFocusY,

        Boolean isActive) {
}
