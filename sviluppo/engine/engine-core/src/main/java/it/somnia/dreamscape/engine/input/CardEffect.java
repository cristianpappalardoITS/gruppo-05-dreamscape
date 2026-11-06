package it.somnia.dreamscape.engine.input;

import java.util.Objects;

/**
 * L'effetto di una carta: il codice del catalogo e il valore X.
 *
 * <p>Il codice è quello di {@code effects.code} (es. {@code WEAKENING}); il comportamento
 * sta nel codice Java del motore. Che il codice esista lo controlla il motore quando gioca la carta.
 *
 * @param code  il codice dell'effetto, non vuoto
 * @param value il valore X ({@code card_effects.effect_value}), {@code null} se l'effetto non ha valore
 */
public record CardEffect(String code, Integer value) {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException     se {@code code} è {@code null}
     * @throws IllegalArgumentException se {@code code} è vuoto o {@code value} è negativo
     */
    public CardEffect {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("Il codice dell'effetto è vuoto");
        }
        if (value != null && value < 0) {
            throw new IllegalArgumentException("Il valore dell'effetto " + code + " è negativo: " + value);
        }
    }
}
