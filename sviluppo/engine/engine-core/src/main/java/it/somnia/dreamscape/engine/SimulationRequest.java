package it.somnia.dreamscape.engine;

import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.GameRules;

import java.util.Objects;

/**
 * La richiesta di una partita: tutto quello che serve al motore, in un'unica volta (decisione 029).
 *
 * <p>È il corpo di {@code POST /api/v1/simulations} di engine-service. Il backend la costruisce con
 * queste stesse classi, perché include il progetto del motore: la forma della chiamata è scritta
 * una volta sola. Contiene solo dati, nessuna dipendenza da HTTP o da Spring.
 *
 * @param challengerDeck il mazzo di chi ha avviato la partita
 * @param opponentDeck   il mazzo dell'avversario
 * @param rules          punti vita, limite di round e vantaggi di fazione
 * @param seed           il seed della partita, scelto e salvato dal backend in {@code matches.random_seed}
 * @param withLog        {@code false} per avere solo il risultato, senza il log (simulazioni, #87)
 */
public record SimulationRequest(DeckSnapshot challengerDeck, DeckSnapshot opponentDeck, GameRules rules,
                                Long seed, boolean withLog) {

    /**
     * Controlla che ci sia tutto.
     *
     * @throws NullPointerException se un mazzo, le regole o il seed sono {@code null}
     */
    public SimulationRequest {
        Objects.requireNonNull(challengerDeck, "challengerDeck");
        Objects.requireNonNull(opponentDeck, "opponentDeck");
        Objects.requireNonNull(rules, "rules");
        // Il seed è un oggetto, non un long: se manca nel JSON la richiesta si ferma, invece di valere 0
        Objects.requireNonNull(seed, "seed");
    }
}
