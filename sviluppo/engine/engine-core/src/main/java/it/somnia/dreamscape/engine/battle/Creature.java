package it.somnia.dreamscape.engine.battle;

import it.somnia.dreamscape.engine.input.CardSnapshot;

import java.util.Objects;

/**
 * Una creatura in campo, con l'attacco e la difesa attuali.
 *
 * <p>La difesa scende con i colpi e resta così nei round successivi dello stesso scontro.
 * L'attacco per ora è quello della carta: le modifiche degli effetti arrivano con la #86.
 */
final class Creature {

    private final CardSnapshot card;
    private final int attack;
    private int defense;

    /**
     * Mette in campo una carta con le sue statistiche.
     *
     * @param card la carta giocata, non {@code null}
     */
    Creature(CardSnapshot card) {
        this.card = Objects.requireNonNull(card, "card");
        this.attack = card.attack();
        this.defense = card.defense();
    }

    CardSnapshot card() {
        return card;
    }

    /**
     * Restituisce l'attacco con le modifiche permanenti, senza bonus di fazione.
     *
     * @return l'attacco, almeno 0
     */
    int attack() {
        return attack;
    }

    int defense() {
        return defense;
    }

    /**
     * Toglie difesa alla creatura. La difesa può scendere sotto 0.
     *
     * @param damage il danno del colpo, almeno 0
     */
    void takeDamage(int damage) {
        defense -= damage;
    }

    /**
     * Dice se la creatura deve morire nella fase finale.
     *
     * @return {@code true} se la difesa è 0 o meno
     */
    boolean isDead() {
        return defense <= 0;
    }
}
