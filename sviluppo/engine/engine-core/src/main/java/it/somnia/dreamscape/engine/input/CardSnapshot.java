package it.somnia.dreamscape.engine.input;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Una carta del mazzo com'era al momento della partita, presa dalla copia del mazzo
 * ({@code matches.*_deck_snapshot}).
 *
 * <p>Contiene solo quello che serve al motore. {@code cardId} è la versione usata nel mazzo
 * (es. la full art) e finisce nel log; statistiche ed effetto sono quelli della carta base.
 *
 * @param cardId   l'id della versione della carta usata nel mazzo
 * @param name     il nome della carta, utile nei test e nelle simulazioni
 * @param attack   l'attacco, almeno 0
 * @param defense  la difesa, almeno 1
 * @param faction  la fazione
 * @param quantity quante copie ci sono nel mazzo, almeno 1
 * @param effects  gli effetti della carta: al massimo uno, lista vuota se non ne ha
 */
public record CardSnapshot(UUID cardId, String name, int attack, int defense, Faction faction, int quantity,
                           List<CardEffect> effects) {

    /**
     * Controlla i valori e rende la lista degli effetti non modificabile.
     *
     * @throws NullPointerException     se un valore obbligatorio è {@code null}
     * @throws IllegalArgumentException se attacco, difesa o quantità sono fuori dai limiti,
     *                                  o se la carta ha più di un effetto
     */
    public CardSnapshot {
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(faction, "faction");
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        if (attack < 0) {
            throw new IllegalArgumentException("L'attacco di " + name + " è negativo: " + attack);
        }
        if (defense < 1) {
            throw new IllegalArgumentException("La difesa di " + name + " è minore di 1: " + defense);
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("La quantità di " + name + " è minore di 1: " + quantity);
        }
        // Al massimo un effetto per carta (decisione 028)
        if (effects.size() > 1) {
            throw new IllegalArgumentException(name + " ha " + effects.size() + " effetti, al massimo 1");
        }
    }
}
