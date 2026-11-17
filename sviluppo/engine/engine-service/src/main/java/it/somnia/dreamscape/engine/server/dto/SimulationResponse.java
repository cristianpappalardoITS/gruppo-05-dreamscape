package it.somnia.dreamscape.engine.server.dto;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.battle.BattleEngine;
import it.somnia.dreamscape.engine.log.MatchLog;

/**
 * La risposta di {@code POST /api/v1/simulations}: il risultato della partita e il log.
 *
 * <p>Il backend salva {@code log} così com'è in {@code matches.round_log}; il formato è in
 * {@code docs/database/schema.md}.
 *
 * @param engineVersion        la versione del motore, anche quando il log è spento
 * @param winner               il lato che ha vinto, {@code null} se pareggio
 * @param endReason            perché è finita la partita
 * @param totalRounds          quanti round sono stati giocati
 * @param challengerLifePoints i punti vita finali di chi ha avviato la partita
 * @param opponentLifePoints   i punti vita finali dell'avversario
 * @param coinTossWinner       chi ha vinto la moneta
 * @param log                  il log round per round, {@code null} se la richiesta aveva {@code withLog: false}
 */
public record SimulationResponse(String engineVersion, Side winner, EndReason endReason, int totalRounds,
                                 int challengerLifePoints, int opponentLifePoints, Side coinTossWinner,
                                 MatchLog log) {

    public static SimulationResponse from(BattleResult result) {
        return new SimulationResponse(BattleEngine.ENGINE_VERSION, result.winner(), result.endReason(),
                result.totalRounds(), result.challengerLifePoints(), result.opponentLifePoints(),
                result.coinTossWinner(), result.log());
    }
}
