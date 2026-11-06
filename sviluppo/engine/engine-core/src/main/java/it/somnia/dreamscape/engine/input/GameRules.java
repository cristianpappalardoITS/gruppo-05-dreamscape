package it.somnia.dreamscape.engine.input;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * I valori di gioco con cui si gioca una partita.
 *
 * <p>Li manda il backend a ogni partita, letti da {@code game_modes} e {@code faction_advantages}:
 * nel motore non ci sono numeri di gioco scritti nel codice.
 *
 * @param startingLifePoints i punti vita iniziali di ogni giocatore, almeno 1
 * @param roundLimit         dopo questo round la partita finisce in pareggio, almeno 1
 * @param factionAdvantages  i vantaggi di fazione, al massimo uno per coppia di fazioni
 */
public record GameRules(int startingLifePoints, int roundLimit, List<FactionAdvantage> factionAdvantages) {

    /**
     * Controlla i valori e rende la lista dei vantaggi non modificabile.
     *
     * @throws NullPointerException     se la lista o un vantaggio sono {@code null}
     * @throws IllegalArgumentException se un valore è fuori dai limiti, se una coppia di fazioni
     *                                  compare due volte o se un vantaggio ha un bonus di difesa
     */
    public GameRules {
        factionAdvantages = List.copyOf(Objects.requireNonNull(factionAdvantages, "factionAdvantages"));
        if (startingLifePoints < 1) {
            throw new IllegalArgumentException("I punti vita iniziali sono minori di 1: " + startingLifePoints);
        }
        if (roundLimit < 1) {
            throw new IllegalArgumentException("Il limite di round è minore di 1: " + roundLimit);
        }
        Set<String> pairs = new HashSet<>();
        for (FactionAdvantage advantage : factionAdvantages) {
            if (!pairs.add(advantage.faction() + "→" + advantage.target())) {
                throw new IllegalArgumentException(
                        "Vantaggio ripetuto: " + advantage.faction() + " → " + advantage.target());
            }
            // Il regolamento prevede solo il bonus di attacco: meglio fermarsi che ignorarlo in silenzio
            if (advantage.defenseBonus() != 0) {
                throw new IllegalArgumentException("Il motore non gestisce il bonus di difesa dei vantaggi di fazione");
            }
        }
    }

    /**
     * Restituisce l'attacco in più di una creatura di {@code attacker} contro una di {@code defender}.
     *
     * @param attacker la fazione della creatura che attacca, non {@code null}
     * @param defender la fazione della creatura colpita, non {@code null}
     * @return il bonus di attacco, oppure 0 se {@code attacker} non è forte contro {@code defender}
     */
    public int attackBonus(Faction attacker, Faction defender) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(defender, "defender");
        for (FactionAdvantage advantage : factionAdvantages) {
            if (advantage.faction() == attacker && advantage.target() == defender) {
                return advantage.attackBonus();
            }
        }
        return 0;
    }
}
