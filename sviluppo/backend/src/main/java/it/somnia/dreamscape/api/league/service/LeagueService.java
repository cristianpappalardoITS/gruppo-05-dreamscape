package it.somnia.dreamscape.api.league.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.league.dto.response.LeagueResponse;
import it.somnia.dreamscape.api.league.model.League;
import it.somnia.dreamscape.api.league.model.UserLeagueReward;
import it.somnia.dreamscape.api.league.repository.LeagueRepository;
import it.somnia.dreamscape.api.league.repository.UserLeagueRewardRepository;
import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.service.CreditService;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Le leghe del rank e i loro premi (decisione 011).
 *
 * <p>Il premio di una lega arriva solo la prima volta che il giocatore ci entra: la riga di
 * {@code user_league_rewards} resta anche se poi scende, quindi chi risale non lo riprende.
 */
@Service
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final UserLeagueRewardRepository userLeagueRewardRepository;
    private final CreditService creditService;

    /**
     * Restituisce le leghe con soglie e premi, dalla più bassa ({@code GET /leagues}).
     *
     * @return le leghe
     */
    @Transactional(readOnly = true)
    public List<LeagueResponse> listLeagues() {
        return leagueRepository.findAllByOrderByMinRatingAsc().stream().map(LeagueResponse::from).toList();
    }

    /**
     * Registra le leghe in cui il giocatore entra per la prima volta con il suo punteggio attuale e gli dà i loro
     * premi ({@code LEAGUE_REWARD}).
     *
     * <p>Si chiama dopo ogni variazione del punteggio. Di solito si entra in una lega sola; più di una se il
     * backoffice ha abbassato le soglie. La lega più bassa è quella di partenza e non dà premi.
     *
     * @param lockedUser il giocatore, con la riga già bloccata ({@link UserService#getActiveUserForUpdate(UUID)})
     *                   e il punteggio già aggiornato
     * @param matchId    la partita che ha cambiato il punteggio
     * @return le leghe raggiunte per la prima volta, dalla più bassa; vuota se nessuna
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UserLeagueReward> grantNewLeagues(User lockedUser, UUID matchId) {
        List<UserLeagueReward> rewards = new ArrayList<>();
        for (League league : leagueRepository.findReachedForTheFirstTime(lockedUser.getId(), lockedUser.getRating())) {
            CreditTransaction transaction = league.getRewardCredits() > 0
                    ? creditService.apply(lockedUser, CreditTransactionType.LEAGUE_REWARD, league.getRewardCredits(),
                            null, matchId)
                    : null;
            rewards.add(userLeagueRewardRepository.save(new UserLeagueReward(lockedUser, league, matchId,
                    transaction)));
        }
        return rewards;
    }
}
