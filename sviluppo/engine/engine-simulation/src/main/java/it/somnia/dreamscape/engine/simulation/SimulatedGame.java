package it.somnia.dreamscape.engine.simulation;

import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.Faction;

import java.util.Objects;

/**
 * Una partita da giocare con ogni combinazione di regole: due mazzi e il seed.
 *
 * <p>Le stesse partite si giocano con tutte le regole, così le differenze tra le righe delle
 * tabelle dipendono solo dalle regole e non dal caso.
 *
 * @param challengerDeck    il mazzo di {@code CHALLENGER}
 * @param challengerProfile il profilo del mazzo di {@code CHALLENGER}
 * @param challengerFaction la fazione del mazzo di {@code CHALLENGER}, {@code null} se misto
 * @param opponentDeck      il mazzo di {@code OPPONENT}
 * @param opponentProfile   il profilo del mazzo di {@code OPPONENT}
 * @param opponentFaction   la fazione del mazzo di {@code OPPONENT}, {@code null} se misto
 * @param seed              il seed della partita
 */
record SimulatedGame(DeckSnapshot challengerDeck, DeckProfile challengerProfile, Faction challengerFaction,
                     DeckSnapshot opponentDeck, DeckProfile opponentProfile, Faction opponentFaction,
                     long seed) {

    SimulatedGame {
        Objects.requireNonNull(challengerDeck, "challengerDeck");
        Objects.requireNonNull(challengerProfile, "challengerProfile");
        Objects.requireNonNull(opponentDeck, "opponentDeck");
        Objects.requireNonNull(opponentProfile, "opponentProfile");
    }

    /**
     * Dice se i due mazzi sono di una sola fazione ciascuno, e di fazioni diverse.
     *
     * @return {@code true} se conta per il vantaggio di fazione
     */
    boolean isFactionMatchup() {
        return challengerFaction != null && opponentFaction != null && challengerFaction != opponentFaction;
    }
}
