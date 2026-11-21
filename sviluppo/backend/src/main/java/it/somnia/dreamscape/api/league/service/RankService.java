package it.somnia.dreamscape.api.league.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.league.service.EloCalculator.RatingChanges;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.repository.MatchRepository;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Il rank delle partite classificate (#106, decisioni 010 e 011): aggiorna il punteggio dei due giocatori con
 * l'Elo e dà i premi delle leghe raggiunte per la prima volta.
 *
 * <p>Lo usa il matchmaking (#127) subito dopo aver giocato e salvato la partita, nella stessa transazione e con le
 * righe dei due giocatori già bloccate ({@link UserService#getActiveUserForUpdate(UUID)}, in ordine di id): lo
 * stesso avversario può essere scelto da più giocatori nello stesso momento, e senza il blocco si perderebbero
 * aggiornamenti del punteggio e si supererebbe il limite giornaliero.
 */
@Service
@RequiredArgsConstructor
public class RankService {

    /**
     * Quante classificate al giorno contro lo stesso avversario cambiano il punteggio (giorno in ora italiana).
     */
    public static final int RATED_MATCHES_PER_DAY_PER_OPPONENT = 3;

    private final MatchRepository matchRepository;
    private final LeagueService leagueService;
    private final GameProperties gameProperties;

    /**
     * Applica al rank il risultato di una partita classificata appena salvata.
     *
     * <p>Se i due giocatori hanno già giocato oggi 3 classificate che hanno cambiato il punteggio, questa non
     * conta: punteggi invariati e variazioni nella partita a {@code null}. Altrimenti aggiorna i due punteggi,
     * salva le variazioni nella partita e registra le leghe raggiunte per la prima volta, con i loro premi.
     *
     * @param match la partita classificata appena salvata, con i due giocatori già bloccati
     * @return le variazioni del punteggio e le leghe raggiunte
     * @throws IllegalArgumentException se la partita non è classificata o ha già le variazioni del punteggio
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public RankResult applyRankedMatch(Match match) {
        if (match.getMatchType() != MatchType.RANKED) {
            throw new IllegalArgumentException("Solo le partite classificate cambiano il rank");
        }
        if (match.getChallengerRatingChange() != null) {
            throw new IllegalArgumentException("La partita " + match.getId() + " ha già cambiato il rank");
        }
        if (hasReachedDailyLimit(match)) {
            return RankResult.notRated();
        }
        User challenger = match.getChallenger();
        User opponent = match.getOpponent();
        RatingChanges changes = EloCalculator.calculate(challenger.getRating(), opponent.getRating(),
                challengerScore(match));
        challenger.setRating(challenger.getRating() + changes.challenger());
        opponent.setRating(opponent.getRating() + changes.opponent());
        match.recordRatingChanges(changes.challenger(), changes.opponent());
        return new RankResult(changes.challenger(), changes.opponent(),
                leagueService.grantNewLeagues(challenger, match.getId()),
                leagueService.grantNewLeagues(opponent, match.getId()));
    }

    // Conta le classificate tra i due, in tutti e due i ruoli, giocate nello stesso giorno (ora italiana) che
    // hanno già cambiato il punteggio. Questa partita non c'è: le sue variazioni sono ancora null
    private boolean hasReachedDailyLimit(Match match) {
        ZoneId zone = gameProperties.timeZone();
        LocalDate day = LocalDate.ofInstant(match.getPlayedAt(), zone);
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        long rated = matchRepository.countRatedBetween(MatchType.RANKED, match.getChallenger().getId(),
                match.getOpponent().getId(), from, to);
        return rated >= RATED_MATCHES_PER_DAY_PER_OPPONENT;
    }

    private static double challengerScore(Match match) {
        if (match.getWinner() == null) {
            return 0.5;
        }
        return match.getWinner().getId().equals(match.getChallenger().getId()) ? 1 : 0;
    }
}
