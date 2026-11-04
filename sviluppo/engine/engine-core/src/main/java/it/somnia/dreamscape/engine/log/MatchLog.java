package it.somnia.dreamscape.engine.log;

import it.somnia.dreamscape.engine.Side;

import java.util.List;
import java.util.Objects;

/**
 * Il log della partita, che il backend salva così com'è in {@code matches.round_log}.
 *
 * @param engineVersion  la versione del motore: con lo stesso seed la partita si rigioca uguale
 *                       solo con la stessa versione
 * @param coinTossWinner chi ha vinto la moneta
 * @param rounds         i round, in ordine
 */
public record MatchLog(String engineVersion, Side coinTossWinner, List<RoundLog> rounds) {

    /**
     * Controlla i valori e rende la lista dei round non modificabile.
     *
     * @throws NullPointerException se un valore o un round sono {@code null}
     */
    public MatchLog {
        Objects.requireNonNull(engineVersion, "engineVersion");
        Objects.requireNonNull(coinTossWinner, "coinTossWinner");
        rounds = List.copyOf(Objects.requireNonNull(rounds, "rounds"));
    }
}
