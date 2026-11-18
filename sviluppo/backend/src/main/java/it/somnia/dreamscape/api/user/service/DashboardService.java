package it.somnia.dreamscape.api.user.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.challenge.service.ChallengeService;
import it.somnia.dreamscape.api.friend.service.FriendService;
import it.somnia.dreamscape.api.pack.dto.response.OpenedCardResponse;
import it.somnia.dreamscape.api.trade.service.TradeService;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.pack.service.OpenedCardMapper;
import it.somnia.dreamscape.api.pack.service.PackOpener;
import it.somnia.dreamscape.api.user.dto.response.DashboardLeagueResponse;
import it.somnia.dreamscape.api.user.dto.response.DashboardResponse;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.LeagueView;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Il riepilogo della Dashboard (GET /me/summary): crediti, punteggio, lega e ultime carte trovate.
 *
 * <p>I contatori delle richieste di amicizia e degli scambi ricevuti in attesa sono veri (#93); quello delle
 * sfide conta le ricevute in attesa e le lanciate con un mazzo da sistemare (#94). Le missioni hanno già il loro
 * posto nella risposta, ma per ora la lista è vuota: si riempie con le missioni (#125).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PackOpeningRepository packOpeningRepository;
    private final OpenedCardMapper openedCardMapper;
    private final FriendService friendService;
    private final TradeService tradeService;
    private final ChallengeService challengeService;

    /**
     * Restituisce il riepilogo dell'utente collegato.
     *
     * @param userId l'utente collegato
     * @return il riepilogo per la Dashboard
     * @throws UserExceptions.AccountDisabledException se l'account è stato cancellato
     */
    @Transactional(readOnly = true)
    public DashboardResponse getSummary(UUID userId) {
        User user = userService.getActiveUser(userId);
        return new DashboardResponse(
                user.getUsername(),
                user.getCredits(),
                user.getRating(),
                leagueOf(user.getRating()),
                user.getRankedDeck() != null,
                List.of(),
                friendService.countPendingReceived(userId),
                tradeService.countPendingReceived(userId),
                challengeService.countNeedingAttention(userId),
                latestCards(userId));
    }

    // La lega c'è sempre: la più bassa parte da 0 punti e il punteggio non scende sotto 0
    private DashboardLeagueResponse leagueOf(int rating) {
        LeagueView league = userRepository.findLeagueForRating(rating).orElseThrow();
        return userRepository.findNextLeague(rating)
                .map(next -> new DashboardLeagueResponse(league.getCode(), league.getName(),
                        next.getName(), next.getMinRating()))
                .orElseGet(() -> new DashboardLeagueResponse(league.getCode(), league.getName(), null, null));
    }

    // Le carte dell'ultima apertura, nell'ordine di uscita; nessuna se l'utente non ha mai aperto pacchetti
    private List<OpenedCardResponse> latestCards(UUID userId) {
        return packOpeningRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(opening -> openedCardMapper.toResponses(PackOpener.cardsOf(opening)))
                .orElseGet(List::of);
    }
}
