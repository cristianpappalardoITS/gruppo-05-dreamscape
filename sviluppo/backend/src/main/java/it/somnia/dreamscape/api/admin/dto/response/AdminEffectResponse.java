package it.somnia.dreamscape.api.admin.dto.response;

import it.somnia.dreamscape.api.card.model.Effect;
import it.somnia.dreamscape.api.card.model.EffectTrigger;

/**
 * Un effetto che si può assegnare a una carta nel backoffice ({@code GET /admin/effects}).
 *
 * @param code         codice dell'effetto, es. PUNISHMENT
 * @param name         nome per i giocatori, es. Castigo
 * @param description  testo per i giocatori; {value} si sostituisce con il valore X della carta
 * @param triggerEvent quando agisce
 * @param hasValue     true se la carta deve avere il valore X (effectValue)
 */
public record AdminEffectResponse(String code, String name, String description, EffectTrigger triggerEvent,
        boolean hasValue) {

    public static AdminEffectResponse from(Effect effect) {
        return new AdminEffectResponse(effect.getCode(), effect.getName(), effect.getDescription(),
                effect.getTriggerEvent(), effect.hasValue());
    }
}
