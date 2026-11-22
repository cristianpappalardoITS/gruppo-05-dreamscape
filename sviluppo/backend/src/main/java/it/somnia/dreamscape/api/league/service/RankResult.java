package it.somnia.dreamscape.api.league.service;

import java.util.List;

import it.somnia.dreamscape.api.league.model.UserLeagueReward;

/**
 * Cosa ha cambiato una partita classificata nel rank dei due giocatori.
 *
 * @param challengerRatingChange variazione del punteggio di chi ha avviato la partita; null se la partita non
 *                               conta per il rank (oltre la terza del giorno contro lo stesso avversario)
 * @param opponentRatingChange   variazione del punteggio dell'avversario; null come sopra
 * @param challengerLeagues      le leghe in cui chi ha avviato la partita entra per la prima volta
 * @param opponentLeagues        le leghe in cui l'avversario entra per la prima volta
 */
public record RankResult(Integer challengerRatingChange, Integer opponentRatingChange,
        List<UserLeagueReward> challengerLeagues, List<UserLeagueReward> opponentLeagues) {

    static RankResult notRated() {
        return new RankResult(null, null, List.of(), List.of());
    }

    public boolean isRated() {
        return challengerRatingChange != null;
    }
}
