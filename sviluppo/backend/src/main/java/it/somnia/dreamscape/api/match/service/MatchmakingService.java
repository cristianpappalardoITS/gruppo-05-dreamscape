package it.somnia.dreamscape.api.match.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.common.config.GameProperties.RankedWinReward;
import it.somnia.dreamscape.api.deck.service.DeckCheck;
import it.somnia.dreamscape.api.deck.service.DeckExceptions.DeckNotFoundException;
import it.somnia.dreamscape.api.deck.service.DeckExceptions.InvalidDeckException;
import it.somnia.dreamscape.api.deck.service.DeckService;
import it.somnia.dreamscape.api.friend.service.FriendService;
import it.somnia.dreamscape.api.league.service.RankService;
import it.somnia.dreamscape.api.match.dto.response.MatchResponse;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.service.MatchExceptions.EngineUnavailableException;
import it.somnia.dreamscape.api.match.service.MatchExceptions.NoOpponentFoundException;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;
import it.somnia.dreamscape.api.user.repository.CreditTransactionRepository;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.CreditService;
import it.somnia.dreamscape.api.user.service.UserExceptions.AccountDisabledException;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Il matchmaking asincrono (#127, decisione 010): chi cerca una partita viene abbinato subito a un avversario, che
 * non deve essere collegato e gioca con il suo mazzo classificato. La partita si gioca e si salva subito.
 *
 * <p>Classificata: avversario con il punteggio più vicino (entro 100, poi 200, poi 400 punti, poi il più vicino),
 * scegliendo per ultimi quelli già affrontati 3 volte oggi, che non darebbero punti. Cambia il rank di tutti e due;
 * chi la avvia e vince prende 30 DC, al massimo 3 vittorie premiate al giorno e niente DC se l'avversario è un
 * amico. Chi viene scelto come avversario e vince prende solo i punti del rank. Rapida: avversario a caso, niente
 * rank né crediti.
 *
 * <p>Prima di giocare blocca le righe dei due giocatori, in ordine di id: lo stesso avversario può essere scelto da
 * più giocatori nello stesso momento, e senza il blocco si perderebbero aggiornamenti del punteggio e si
 * supererebbero i limiti giornalieri.
 */
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    // Le distanze di punteggio entro cui si cerca l'avversario della classificata, in ordine; oltre l'ultima si
    // prende il più vicino
    private static final List<Integer> RATING_STEPS = List.of(100, 200, 400);

    private final UserRepository userRepository;
    private final UserService userService;
    private final DeckService deckService;
    private final FriendService friendService;
    private final MatchService matchService;
    private final RankService rankService;
    private final CreditService creditService;
    private final CreditTransactionRepository creditTransactionRepository;
    private final GameProperties gameProperties;
    private final RandomGenerator randomGenerator;
    private final Clock clock;

    /**
     * Cerca un avversario con un punteggio vicino e gioca una partita classificata ({@code POST /matches/ranked}).
     *
     * @param userId chi cerca, l'utente collegato
     * @param deckId un suo mazzo valido
     * @return il risultato della partita, come {@code GET /matches/{matchId}}
     * @throws DeckNotFoundException      se il mazzo non esiste o è di un altro utente
     * @throws InvalidDeckException       se il mazzo non è valido, con tutti i problemi
     * @throws NoOpponentFoundException   se nessuno ha un mazzo classificato valido
     * @throws EngineUnavailableException se il motore non risponde: non si salva niente
     */
    @Transactional
    public MatchResponse playRanked(UUID userId, UUID deckId) {
        return play(MatchType.RANKED, userId, deckId);
    }

    /**
     * Cerca un avversario a caso e gioca una partita rapida ({@code POST /matches/casual}).
     *
     * @param userId chi cerca, l'utente collegato
     * @param deckId un suo mazzo valido
     * @return il risultato della partita, come {@code GET /matches/{matchId}}
     * @throws DeckNotFoundException      se il mazzo non esiste o è di un altro utente
     * @throws InvalidDeckException       se il mazzo non è valido, con tutti i problemi
     * @throws NoOpponentFoundException   se nessuno ha un mazzo classificato valido
     * @throws EngineUnavailableException se il motore non risponde: non si salva niente
     */
    @Transactional
    public MatchResponse playCasual(UUID userId, UUID deckId) {
        return play(MatchType.CASUAL, userId, deckId);
    }

    private MatchResponse play(MatchType matchType, UUID userId, UUID deckId) {
        User player = userService.getActiveUser(userId);
        DeckCheck playerDeck = deckService.checkOwnDeck(userId, deckId);
        if (!playerDeck.isValid()) {
            throw new InvalidDeckException(playerDeck.problems());
        }
        // Il primo candidato con il mazzo classificato ancora valido: i mazzi si controllano uno alla volta
        User opponent = candidates(matchType, player).stream()
                .filter(candidate -> deckService.checkDeck(candidate.getRankedDeck()).isValid())
                .findFirst()
                .orElseThrow(NoOpponentFoundException::new);
        lockInIdOrder(player, opponent);
        // Dopo il blocco si ricontrolla: nel frattempo l'avversario può aver cambiato o tolto il mazzo classificato
        DeckCheck opponentDeck = opponent.getRankedDeck() == null ? null
                : deckService.checkDeck(opponent.getRankedDeck());
        if (opponentDeck == null || !opponentDeck.isValid()) {
            throw new NoOpponentFoundException();
        }
        Match match = matchService.play(matchType, player, playerDeck, opponent, opponentDeck, null);
        if (matchType == MatchType.RANKED) {
            rankService.applyRankedMatch(match);
            rewardWin(match);
        }
        return matchService.getMatch(userId, match.getId());
    }

    // Classificata: prima chi non è al limite di oggi, poi per fascia di distanza; oltre l'ultima fascia il più
    // vicino. A parità resta l'ordine mescolato, così non esce sempre lo stesso avversario
    private List<User> candidates(MatchType matchType, User player) {
        List<User> candidates = new ArrayList<>(
                userRepository.findByEnabledTrueAndRoleAndRankedDeckIsNotNullAndIdNot(UserRole.PLAYER,
                        player.getId()));
        Collections.shuffle(candidates, randomGenerator);
        if (matchType == MatchType.CASUAL) {
            return candidates;
        }
        Set<UUID> atDailyLimit = rankService.opponentsAtDailyLimit(player.getId(), Instant.now(clock));
        int rating = player.getRating();
        candidates.sort(Comparator.comparing((User candidate) -> atDailyLimit.contains(candidate.getId()))
                .thenComparingInt(candidate -> ratingStep(Math.abs(candidate.getRating() - rating)))
                .thenComparingInt(candidate -> farDistance(Math.abs(candidate.getRating() - rating))));
        return candidates;
    }

    // 0 entro 100 punti, 1 entro 200, 2 entro 400, 3 oltre
    private static int ratingStep(int distance) {
        for (int step = 0; step < RATING_STEPS.size(); step++) {
            if (distance <= RATING_STEPS.get(step)) {
                return step;
            }
        }
        return RATING_STEPS.size();
    }

    // Dentro le fasce conta solo l'ordine mescolato; oltre l'ultima vince il più vicino
    private static int farDistance(int distance) {
        return distance > RATING_STEPS.getLast() ? distance : 0;
    }

    private void lockInIdOrder(User player, User opponent) {
        if (player.getId().compareTo(opponent.getId()) < 0) {
            userService.getActiveUserForUpdate(player.getId());
            lockOpponent(opponent);
        } else {
            lockOpponent(opponent);
            userService.getActiveUserForUpdate(player.getId());
        }
    }

    // Se l'avversario ha appena cancellato l'account, per chi cerca è come non aver trovato nessuno
    private void lockOpponent(User opponent) {
        try {
            userService.getActiveUserForUpdate(opponent.getId());
        } catch (AccountDisabledException e) {
            throw new NoOpponentFoundException();
        }
    }

    // I DC della vittoria vanno solo a chi ha avviato la classificata, se l'avversario non è un amico e se non ha
    // già il massimo di vittorie premiate nel giorno della partita (ora italiana)
    private void rewardWin(Match match) {
        User player = match.getChallenger();
        if (match.getWinner() == null || !match.getWinner().getId().equals(player.getId())) {
            return;
        }
        if (friendService.areFriends(player.getId(), match.getOpponent().getId())) {
            return;
        }
        RankedWinReward rankedWin = gameProperties.rankedWin();
        ZoneId zone = gameProperties.timeZone();
        LocalDate day = LocalDate.ofInstant(match.getPlayedAt(), zone);
        long rewardedWins = creditTransactionRepository.countOfTypeBetween(player.getId(),
                CreditTransactionType.MATCH_REWARD, day.atStartOfDay(zone).toInstant(),
                day.plusDays(1).atStartOfDay(zone).toInstant());
        if (rewardedWins < rankedWin.maxPerDay()) {
            creditService.apply(player, CreditTransactionType.MATCH_REWARD, rankedWin.reward(), null, match.getId());
        }
    }
}
