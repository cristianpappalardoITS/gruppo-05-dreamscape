package it.somnia.dreamscape.api.match.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.SimulationRequest;
import it.somnia.dreamscape.engine.battle.BattleEngine;
import it.somnia.dreamscape.engine.log.EventType;
import it.somnia.dreamscape.engine.log.GameEvent;
import it.somnia.dreamscape.engine.log.Phase;
import tools.jackson.databind.json.JsonMapper;

/**
 * Il motore di gioco dentro il backend, al posto di engine-service: nei test le partite si giocano davvero, senza
 * il container del motore. Il log diventa JSON come lo scrive engine-service (GameEventMixin), così il backend
 * salva e rilegge lo stesso formato.
 */
public class InProcessBattleSimulator implements BattleSimulator {

    private final BattleEngine engine = new BattleEngine();
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .addMixIn(GameEvent.class, GameEventMixin.class)
            .build();

    // Per i test del motore spento: come HttpBattleSimulator quando engine-service non risponde
    private volatile boolean unavailable;

    public void setUnavailable(boolean unavailable) {
        this.unavailable = unavailable;
    }

    @Override
    public SimulationResult simulate(SimulationRequest request) {
        if (unavailable) {
            throw new MatchExceptions.EngineUnavailableException();
        }
        BattleResult result = engine.simulate(request);
        return new SimulationResult(BattleEngine.ENGINE_VERSION, result.winner(), result.endReason(),
                result.totalRounds(), result.challengerLifePoints(), result.opponentLifePoints(),
                result.coinTossWinner(), result.log() == null ? null : jsonMapper.valueToTree(result.log()));
    }

    // Come in engine-service: phase e type sono metodi degli eventi, non campi, e vanno scritti per primi
    @JsonPropertyOrder({ "phase", "type" })
    interface GameEventMixin {

        @JsonProperty("phase")
        Phase phase();

        @JsonProperty("type")
        EventType type();
    }
}
