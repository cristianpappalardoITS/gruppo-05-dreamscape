package it.somnia.dreamscape.api.league.dto.response;

import it.somnia.dreamscape.api.league.model.League;

/**
 * Una lega del rank ({@code GET /leagues} e backoffice).
 *
 * @param code          codice fisso, es. DREAMER
 * @param name          nome della lega, es. Sognatore
 * @param minRating     punteggio da cui si entra nella lega; la più bassa parte da 0
 * @param rewardCredits Dream Credits la prima volta che si entra nella lega; 0 = nessun premio
 */
public record LeagueResponse(String code, String name, int minRating, int rewardCredits) {

    public static LeagueResponse from(League league) {
        return new LeagueResponse(league.getCode(), league.getName(), league.getMinRating(),
                league.getRewardCredits());
    }
}
