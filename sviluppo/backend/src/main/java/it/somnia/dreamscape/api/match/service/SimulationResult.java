package it.somnia.dreamscape.api.match.service;

import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;
import tools.jackson.databind.JsonNode;

/**
 * Il risultato di una partita giocata dal motore, come lo restituisce engine-service.
 *
 * <p>I lati sono {@code CHALLENGER} (chi ha avviato la partita) e {@code OPPONENT}: il backend li traduce negli
 * utenti. Crediti, rank, missioni e leghe li calcola il backend dopo la partita.
 *
 * @param engineVersion        la versione del motore che ha giocato la partita
 * @param winner               il lato che ha vinto, {@code null} se pareggio
 * @param endReason            perché è finita: {@code LIFE_POINTS}, {@code DECK_EXHAUSTED}, {@code ROUND_LIMIT}
 * @param totalRounds          quanti round sono stati giocati
 * @param challengerLifePoints i punti vita finali di chi ha avviato la partita
 * @param opponentLifePoints   i punti vita finali dell'avversario
 * @param coinTossWinner       chi ha vinto la moneta
 * @param log                  il log round per round, da salvare così com'è in {@code matches.round_log}
 *                             (formato in docs/database/schema.md); {@code null} se è stato chiesto senza log
 */
public record SimulationResult(String engineVersion, Side winner, EndReason endReason, int totalRounds,
        int challengerLifePoints, int opponentLifePoints, Side coinTossWinner, JsonNode log) {

    public SimulationResult {
        // "log": null nel JSON arriva come nodo vuoto: qui diventa null, come quando il log non c'è
        log = log == null || log.isNull() ? null : log;
    }

    public boolean isDraw() {
        return winner == null;
    }
}
