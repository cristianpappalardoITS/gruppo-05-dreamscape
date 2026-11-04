package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.Objects;
import java.util.UUID;

/**
 * Un colpo del combattimento. In ogni round ce ne sono due, contemporanei; nel log viene prima
 * il lato che ha vinto la moneta.
 *
 * <p>Il danno al giocatore si calcola da qui: {@code attack + factionBonus + attackModifier}
 * della vincitrice meno il {@code defenseBefore} della sconfitta, mai sotto 0 (decisione 029, punto a).
 *
 * @param side           il lato che colpisce
 * @param cardId         la creatura che colpisce
 * @param attack         l'attacco della creatura, con le modifiche permanenti (es. Intensificazione)
 * @param factionBonus   l'attacco in più per il vantaggio di fazione, 0 se non c'è o è bloccato
 * @param attackModifier quanto gli effetti cambiano l'attacco solo in questo round (es. Indebolimento)
 * @param damage         il danno inflitto davvero, almeno 0 (0 se annullato da Barriera)
 * @param defenseBefore  la difesa della creatura colpita prima del colpo
 * @param defenseAfter   la difesa della creatura colpita dopo il colpo; può essere sotto 0
 */
public record Attack(Side side, UUID cardId, int attack, int factionBonus, int attackModifier, int damage,
                     int defenseBefore, int defenseAfter) implements GameEvent {

    /**
     * Controlla i valori.
     *
     * @throws NullPointerException     se {@code side} o {@code cardId} sono {@code null}
     * @throws IllegalArgumentException se il danno è negativo
     */
    public Attack {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(cardId, "cardId");
        if (damage < 0) {
            throw new IllegalArgumentException("Il danno di un colpo è negativo: " + damage);
        }
    }

    @Override
    public Phase phase() {
        return Phase.COMBAT;
    }

    @Override
    public EventType type() {
        return EventType.ATTACK;
    }
}
