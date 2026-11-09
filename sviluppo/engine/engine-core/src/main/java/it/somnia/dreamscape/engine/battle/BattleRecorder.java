package it.somnia.dreamscape.engine.battle;

import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.log.AfterRound;
import it.somnia.dreamscape.engine.log.GameEvent;
import it.somnia.dreamscape.engine.log.MatchLog;
import it.somnia.dreamscape.engine.log.RoundLog;
import it.somnia.dreamscape.engine.log.RoundOutcome;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Raccoglie gli eventi della partita e costruisce il {@link MatchLog}.
 *
 * <p>Con il log spento (simulazioni, #87) non salva niente e non crea neanche gli eventi:
 * {@link BattleEngine} li passa come {@link Supplier}, che qui non viene chiamato.
 */
final class BattleRecorder {

    private final boolean enabled;
    private final List<RoundLog> rounds = new ArrayList<>();
    private final List<GameEvent> currentEvents = new ArrayList<>();

    /**
     * Crea il registratore.
     *
     * @param enabled {@code false} per non salvare il log
     */
    BattleRecorder(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Aggiunge un evento al round in corso.
     *
     * @param event chi crea l'evento, chiamato solo se il log è acceso
     */
    void record(Supplier<? extends GameEvent> event) {
        if (enabled) {
            currentEvents.add(event.get());
        }
    }

    /**
     * Chiude il round in corso con il suo risultato e lo stato dei due lati.
     *
     * @param round      il numero del round, da 1
     * @param clash      il numero dello scontro, da 1
     * @param outcome    chi è morto nel round
     * @param afterRound chi crea lo stato dei due lati, chiamato solo se il log è acceso
     */
    void endRound(int round, int clash, RoundOutcome outcome, Supplier<AfterRound> afterRound) {
        if (enabled) {
            rounds.add(new RoundLog(round, clash, currentEvents, outcome, afterRound.get()));
            currentEvents.clear();
        }
    }

    /**
     * Restituisce il log della partita.
     *
     * @param engineVersion  la versione del motore
     * @param coinTossWinner chi ha vinto la moneta
     * @return il log, {@code null} se è spento
     */
    MatchLog build(String engineVersion, Side coinTossWinner) {
        if (!enabled) {
            return null;
        }
        return new MatchLog(engineVersion, coinTossWinner, rounds);
    }
}
