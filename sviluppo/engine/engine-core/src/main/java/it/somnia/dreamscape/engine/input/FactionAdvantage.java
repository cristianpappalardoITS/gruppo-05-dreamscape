package it.somnia.dreamscape.engine.input;

import java.util.Objects;

/**
 * Un vantaggio di fazione: {@code faction} è forte contro {@code target}.
 *
 * <p>È una riga di {@code faction_advantages}. Il bonus vale per la creatura di {@code faction}
 * quando affronta una creatura di {@code target}.
 *
 * @param faction      la fazione avvantaggiata
 * @param target       la fazione contro cui è forte, diversa da {@code faction}
 * @param attackBonus  l'attacco in più, almeno 0
 * @param defenseBonus la difesa in più, almeno 0
 */
public record FactionAdvantage(Faction faction, Faction target, int attackBonus, int defenseBonus) {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException     se una fazione è {@code null}
     * @throws IllegalArgumentException se le fazioni sono uguali o un bonus è negativo
     */
    public FactionAdvantage {
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(target, "target");
        if (faction == target) {
            throw new IllegalArgumentException("Una fazione non può avere un vantaggio su se stessa: " + faction);
        }
        if (attackBonus < 0 || defenseBonus < 0) {
            throw new IllegalArgumentException("Bonus negativo nel vantaggio " + faction + " → " + target);
        }
    }
}
