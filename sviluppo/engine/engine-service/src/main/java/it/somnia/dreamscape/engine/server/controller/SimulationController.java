package it.somnia.dreamscape.engine.server.controller;

import it.somnia.dreamscape.engine.SimulationRequest;
import it.somnia.dreamscape.engine.battle.BattleEngine;
import it.somnia.dreamscape.engine.server.dto.SimulationResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * L'unica API del motore: gioca una partita e ne restituisce il risultato. La chiave la controlla
 * {@link it.somnia.dreamscape.engine.server.config.EngineKeyFilter} prima di arrivare qui.
 */
@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {

    private final BattleEngine battleEngine;

    public SimulationController(BattleEngine battleEngine) {
        this.battleEngine = battleEngine;
    }

    @PostMapping
    public SimulationResponse simulate(@RequestBody SimulationRequest request) {
        return SimulationResponse.from(battleEngine.simulate(request));
    }
}
