package it.somnia.dreamscape.api.card.dto.response;

import it.somnia.dreamscape.api.card.model.CardEffect;

// L'effetto di una carta: text è il testo per i giocatori con il valore X già inserito
public record CardEffectResponse(String code, String name, Integer value, String text) {

    public static CardEffectResponse from(CardEffect cardEffect) {
        Integer value = cardEffect.getEffectValue();
        String description = cardEffect.getEffect().getDescription();
        String text = value == null ? description : description.replace("{value}", value.toString());
        return new CardEffectResponse(cardEffect.getEffect().getCode(), cardEffect.getEffect().getName(), value, text);
    }
}
