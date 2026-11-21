package it.somnia.dreamscape.api.match.dto.response;

import java.time.Instant;
import java.util.UUID;

import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import it.somnia.dreamscape.engine.EndReason;

/**
 * Una partita nello storico ({@code GET /me/matches}), vista da chi guarda. Il replay è in
 * {@code GET /matches/{matchId}}.
 *
 * @param id           l'id della partita
 * @param matchType    CASUAL (rapida o sfida tra amici) o RANKED
 * @param opponent     l'altro giocatore
 * @param startedByMe  true se l'ha avviata chi guarda (sfida lanciata o partita cercata)
 * @param result       WIN, LOSS o DRAW per chi guarda
 * @param ratingChange variazione del punteggio di chi guarda; null se la partita non conta per il rank
 * @param endReason    LIFE_POINTS, DECK_EXHAUSTED o ROUND_LIMIT
 * @param totalRounds  quanti round
 * @param playedAt     quando è stata giocata
 */
public record MatchSummaryResponse(
        UUID id,
        MatchType matchType,
        UserSummaryResponse opponent,
        boolean startedByMe,
        Result result,
        Integer ratingChange,
        EndReason endReason,
        int totalRounds,
        Instant playedAt) {

    /**
     * Com'è andata per chi guarda.
     */
    public enum Result {
        WIN,
        LOSS,
        DRAW
    }

    public static MatchSummaryResponse from(Match match, UUID userId) {
        boolean startedByMe = match.getChallenger().getId().equals(userId);
        Result result = match.getWinner() == null ? Result.DRAW
                : match.getWinner().getId().equals(userId) ? Result.WIN : Result.LOSS;
        return new MatchSummaryResponse(
                match.getId(),
                match.getMatchType(),
                UserSummaryResponse.from(startedByMe ? match.getOpponent() : match.getChallenger()),
                startedByMe,
                result,
                startedByMe ? match.getChallengerRatingChange() : match.getOpponentRatingChange(),
                match.getEndReason(),
                match.getTotalRounds(),
                match.getPlayedAt());
    }
}
