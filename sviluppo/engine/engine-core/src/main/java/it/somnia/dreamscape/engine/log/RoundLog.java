package it.somnia.dreamscape.engine.log;

import java.util.List;
import java.util.Objects;

/**
 * Un round della partita: gli eventi in ordine, chi è morto e lo stato dei due lati alla fine.
 *
 * @param round      il numero del round, da 1
 * @param clash      il numero dello scontro (la coppia di creature in campo), da 1
 * @param events     gli eventi del round, in ordine
 * @param outcome    chi è morto nel round
 * @param afterRound lo stato dei due lati a fine round
 */
public record RoundLog(int round, int clash, List<GameEvent> events, RoundOutcome outcome, AfterRound afterRound) {

    /**
     * Controlla i valori e rende la lista degli eventi non modificabile.
     *
     * @throws NullPointerException     se un valore o un evento sono {@code null}
     * @throws IllegalArgumentException se il numero del round o dello scontro è minore di 1
     */
    public RoundLog {
        events = List.copyOf(Objects.requireNonNull(events, "events"));
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(afterRound, "afterRound");
        if (round < 1 || clash < 1) {
            throw new IllegalArgumentException("Round e scontro partono da 1: round " + round + ", scontro " + clash);
        }
    }
}
