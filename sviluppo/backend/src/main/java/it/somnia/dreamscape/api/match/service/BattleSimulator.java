package it.somnia.dreamscape.api.match.service;

import it.somnia.dreamscape.api.match.service.MatchExceptions.EngineUnavailableException;
import it.somnia.dreamscape.engine.SimulationRequest;

/**
 * Gioca una partita con il motore di gioco (decisione 029). Il backend non conosce le regole del gioco:
 * prepara i due mazzi, le regole e il seed, chiede la partita e salva il risultato.
 *
 * <p>Oggi l'unica implementazione è {@link HttpBattleSimulator}, che chiama engine-service. Su AWS il motore
 * potrebbe diventare una Lambda: basterebbe un'altra implementazione, senza toccare chi la usa.
 */
public interface BattleSimulator {

    /**
     * Gioca una partita.
     *
     * @param request i due mazzi (le stesse copie salvate in {@code matches.*_deck_snapshot}), le regole lette da
     *                {@code game_modes} e {@code faction_advantages}, il seed salvato in {@code matches.random_seed}
     *                e se serve il log
     * @return il risultato della partita
     * @throws EngineUnavailableException se il motore non risponde o risponde con un errore: chi chiama non deve
     *                                    salvare niente né dare crediti o punti
     */
    SimulationResult simulate(SimulationRequest request);
}
