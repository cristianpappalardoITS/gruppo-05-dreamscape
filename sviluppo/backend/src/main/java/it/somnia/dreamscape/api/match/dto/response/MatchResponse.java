package it.somnia.dreamscape.api.match.dto.response;

import java.time.Instant;
import java.util.UUID;

import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import it.somnia.dreamscape.engine.EndReason;
import tools.jackson.databind.JsonNode;

/**
 * Il risultato di una partita ({@code GET /matches/{matchId}}), come nella guida per il frontend (Partite).
 *
 * @param id                 l'id della partita
 * @param matchType          CASUAL (rapida o sfida tra amici) o RANKED
 * @param challenger         chi ha avviato la partita (chi ha lanciato la sfida)
 * @param opponent           l'avversario
 * @param winnerId           chi ha vinto; null = pareggio
 * @param ratingChange       variazione del rank dei due; null se la partita non conta per il rank
 * @param creditsEarned      i Dream Credits vinti da chi guarda
 * @param leagueReached      la lega raggiunta per la prima volta con questa partita; null se nessuna
 * @param endReason          LIFE_POINTS, DECK_EXHAUSTED o ROUND_LIMIT
 * @param totalRounds        quanti round
 * @param startingLifePoints i punti vita iniziali della modalità
 * @param finalLifePoints    i punti vita finali dei due
 * @param coinTossWinner     chi ha vinto la moneta: CHALLENGER o OPPONENT
 * @param playedAt           quando è stata giocata
 * @param engineVersion      la versione del motore che l'ha giocata
 * @param rounds             il replay: i round con gli eventi in ordine (formato in schema.md)
 * @param decks              le copie dei due mazzi al momento della partita
 */
public record MatchResponse(
        UUID id,
        MatchType matchType,
        UserSummaryResponse challenger,
        UserSummaryResponse opponent,
        UUID winnerId,
        SidesResponse ratingChange,
        int creditsEarned,
        LeagueReachedResponse leagueReached,
        EndReason endReason,
        int totalRounds,
        int startingLifePoints,
        SidesResponse finalLifePoints,
        String coinTossWinner,
        Instant playedAt,
        String engineVersion,
        JsonNode rounds,
        DecksResponse decks) {

    /**
     * Un valore per ciascuno dei due giocatori.
     */
    public record SidesResponse(int challenger, int opponent) {
    }

    /**
     * La lega raggiunta per la prima volta e il suo premio (classificate, #127).
     */
    public record LeagueReachedResponse(String name, int rewardCredits) {
    }

    /**
     * Le copie dei due mazzi (formato di matches.*_deck_snapshot in schema.md).
     */
    public record DecksResponse(JsonNode challenger, JsonNode opponent) {
    }
}
