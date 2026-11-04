package it.somnia.dreamscape.engine;

import it.somnia.dreamscape.engine.log.MatchLog;

import java.util.Objects;

/**
 * Il risultato di una partita simulata: serve al backend per riempire la riga di {@code matches}.
 *
 * <p>Crediti, rank, missioni e leghe non sono qui: li calcola il backend dopo la partita.
 *
 * @param winner               il lato che ha vinto, {@code null} se pareggio
 * @param endReason            perché è finita la partita
 * @param totalRounds          quanti round sono stati giocati, almeno 1
 * @param challengerLifePoints i punti vita finali di chi ha avviato la partita, almeno 0
 * @param opponentLifePoints   i punti vita finali dell'avversario, almeno 0
 * @param coinTossWinner       chi ha vinto la moneta
 * @param log                  il log round per round, {@code null} se è stato spento (simulazioni)
 */
public record BattleResult(Side winner, EndReason endReason, int totalRounds, int challengerLifePoints,
                           int opponentLifePoints, Side coinTossWinner, MatchLog log) {

    /**
     * Controlla che il risultato sia coerente.
     *
     * @throws NullPointerException     se {@code endReason} o {@code coinTossWinner} sono {@code null}
     * @throws IllegalArgumentException se un valore è fuori dai limiti, se al limite di round c'è un vincitore
     *                                  o se il log non ha un round per ogni round giocato
     */
    public BattleResult {
        Objects.requireNonNull(endReason, "endReason");
        Objects.requireNonNull(coinTossWinner, "coinTossWinner");
        if (totalRounds < 1) {
            throw new IllegalArgumentException("Una partita dura almeno un round: " + totalRounds);
        }
        if (challengerLifePoints < 0 || opponentLifePoints < 0) {
            throw new IllegalArgumentException("I punti vita finali non possono essere negativi");
        }
        if (endReason == EndReason.ROUND_LIMIT && winner != null) {
            throw new IllegalArgumentException("Al limite di round la partita è sempre pari");
        }
        if (log != null && log.rounds().size() != totalRounds) {
            throw new IllegalArgumentException(
                    "Il log ha " + log.rounds().size() + " round, la partita " + totalRounds);
        }
    }

    /**
     * Dice se la partita è finita in pareggio.
     *
     * @return {@code true} se non c'è un vincitore
     */
    public boolean isDraw() {
        return winner == null;
    }
}
