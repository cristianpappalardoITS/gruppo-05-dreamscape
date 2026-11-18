package it.somnia.dreamscape.api.challenge.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.challenge.dto.request.AcceptChallengeRequest;
import it.somnia.dreamscape.api.challenge.dto.request.ChallengeRequest;
import it.somnia.dreamscape.api.challenge.dto.response.AcceptChallengeResponse;
import it.somnia.dreamscape.api.challenge.dto.response.ChallengeResponse;
import it.somnia.dreamscape.api.challenge.model.Challenge;
import it.somnia.dreamscape.api.challenge.model.ChallengeStatus;
import it.somnia.dreamscape.api.challenge.repository.ChallengeRepository;
import it.somnia.dreamscape.api.challenge.service.ChallengeExceptions.ChallengeActionNotAllowedException;
import it.somnia.dreamscape.api.challenge.service.ChallengeExceptions.ChallengeAlreadyPendingException;
import it.somnia.dreamscape.api.challenge.service.ChallengeExceptions.ChallengeCancelledException;
import it.somnia.dreamscape.api.challenge.service.ChallengeExceptions.ChallengeNotFoundException;
import it.somnia.dreamscape.api.challenge.service.ChallengeExceptions.ChallengeNotPendingException;
import it.somnia.dreamscape.api.challenge.service.ChallengeExceptions.ChallengerDeckInvalidException;
import it.somnia.dreamscape.api.challenge.service.ChallengeExceptions.InvalidChallengeException;
import it.somnia.dreamscape.api.deck.service.DeckCheck;
import it.somnia.dreamscape.api.deck.service.DeckExceptions.DeckNotFoundException;
import it.somnia.dreamscape.api.deck.service.DeckExceptions.InvalidDeckException;
import it.somnia.dreamscape.api.deck.service.DeckService;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.NotFriendsException;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.UserNotFoundException;
import it.somnia.dreamscape.api.friend.service.FriendService;
import it.somnia.dreamscape.api.friend.service.FriendshipRemovedEvent;
import it.somnia.dreamscape.api.match.model.GameMode;
import it.somnia.dreamscape.api.match.model.Match;
import it.somnia.dreamscape.api.match.model.MatchType;
import it.somnia.dreamscape.api.match.repository.GameModeRepository;
import it.somnia.dreamscape.api.match.service.MatchExceptions.EngineUnavailableException;
import it.somnia.dreamscape.api.match.service.MatchService;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.AccountDeletedEvent;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Le sfide tra amici ({@code /challenges}, decisioni 010 e 026): sono sempre partite rapide, senza rank né
 * crediti. Tra due amici c'è al massimo una sfida in attesa; le sfide non scadono e chi sfida può annullarle.
 *
 * <p>All'accettazione si ricontrollano l'amicizia e i due mazzi, poi la partita si gioca subito. Se il mazzo di chi
 * ha sfidato non è più valido la sfida resta in attesa: lo vedono tutti e due nelle liste, con i problemi.
 */
@Service
@RequiredArgsConstructor
public class ChallengeService {

    private static final String BOX_RECEIVED = "received";
    private static final String DELETED_DECK_PROBLEM = "Il mazzo della sfida è stato cancellato.";

    private final ChallengeRepository challengeRepository;
    private final GameModeRepository gameModeRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final FriendService friendService;
    private final DeckService deckService;
    private final MatchService matchService;
    private final Clock clock;

    /**
     * Le sfide in attesa dell'utente ({@code GET /challenges}), dalla più recente.
     *
     * @param userId l'utente collegato
     * @param box    {@code received}: le sfide da accettare o rifiutare; {@code sent}: quelle lanciate
     * @return le sfide, ognuna con lo stato del mazzo di chi ha sfidato
     */
    @Transactional(readOnly = true)
    public List<ChallengeResponse> listChallenges(UUID userId, String box) {
        userService.getActiveUser(userId);
        List<Challenge> challenges = BOX_RECEIVED.equals(box)
                ? challengeRepository.findByOpponent_IdAndStatusOrderByCreatedAtDesc(userId, ChallengeStatus.PENDING)
                : challengeRepository.findByChallenger_IdAndStatusOrderByCreatedAtDesc(userId,
                        ChallengeStatus.PENDING);
        return challenges.stream().map(this::toResponse).toList();
    }

    /**
     * Lancia una sfida a un amico ({@code POST /challenges}).
     *
     * @param userId  chi sfida, l'utente collegato
     * @param request l'amico e il mazzo
     * @return la sfida in attesa
     * @throws UserNotFoundException            se l'amico non esiste o ha cancellato l'account
     * @throws InvalidChallengeException        se si sfida se stessi
     * @throws NotFriendsException              se i due non sono amici
     * @throws DeckNotFoundException            se il mazzo non esiste o è di un altro utente
     * @throws InvalidDeckException             se il mazzo non è valido, con tutti i problemi
     * @throws ChallengeAlreadyPendingException se c'è già una sfida in attesa tra i due
     */
    @Transactional
    public ChallengeResponse createChallenge(UUID userId, ChallengeRequest request) {
        User challenger = userService.getActiveUser(userId);
        if (userId.equals(request.opponentId())) {
            throw new InvalidChallengeException("Non puoi sfidare te stesso.");
        }
        User opponent = userRepository.findById(request.opponentId())
                .filter(User::isEnabled)
                .orElseThrow(UserNotFoundException::new);
        if (!friendService.areFriends(userId, opponent.getId())) {
            throw new NotFriendsException();
        }
        DeckCheck deck = deckService.checkOwnDeck(userId, request.deckId());
        if (!deck.isValid()) {
            throw new InvalidDeckException(deck.problems());
        }
        if (challengeRepository.existsPendingBetween(userId, opponent.getId())) {
            throw new ChallengeAlreadyPendingException();
        }
        GameMode mode = gameModeRepository.findByCode(GameMode.BASE_CODE)
                .orElseThrow(() -> new IllegalStateException("Manca la modalità " + GameMode.BASE_CODE));
        Challenge challenge = new Challenge(challenger, opponent, deck.deck(), mode);
        try {
            // saveAndFlush: la data della sfida la scrive Hibernate all'inserimento e serve nella risposta
            challengeRepository.saveAndFlush(challenge);
        } catch (DataIntegrityViolationException e) {
            // Due sfide tra gli stessi amici nello stesso momento: il database ferma la seconda
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains("uq_challenges_pending_pair")) {
                throw new ChallengeAlreadyPendingException();
            }
            throw e;
        }
        return toResponse(challenge, deck);
    }

    /**
     * Accetta una sfida e gioca la partita ({@code POST /challenges/{challengeId}/accept}). Solo chi è stato sfidato.
     *
     * <p>Se i due non sono più amici la sfida si annulla. Se il mazzo di chi ha sfidato non è più valido, o se il
     * motore non risponde, non si gioca e la sfida resta in attesa.
     *
     * @param userId      chi accetta, l'utente collegato
     * @param challengeId la sfida
     * @param request     il mazzo di chi accetta
     * @return l'id della partita giocata
     * @throws ChallengeNotFoundException          se la sfida non esiste o non riguarda l'utente
     * @throws ChallengeActionNotAllowedException  se l'utente è chi ha lanciato la sfida
     * @throws ChallengeNotPendingException        se la sfida è già chiusa
     * @throws ChallengeCancelledException         se i due non sono più amici (la sfida si annulla)
     * @throws DeckNotFoundException               se il mazzo scelto non esiste o è di un altro utente
     * @throws InvalidDeckException                se il mazzo scelto non è valido
     * @throws ChallengerDeckInvalidException      se il mazzo di chi ha sfidato non è più valido
     * @throws EngineUnavailableException          se il motore non risponde
     */
    @Transactional(noRollbackFor = ChallengeCancelledException.class)
    public AcceptChallengeResponse acceptChallenge(UUID userId, UUID challengeId, AcceptChallengeRequest request) {
        userService.getActiveUser(userId);
        Challenge challenge = pendingChallengeForUpdate(userId, challengeId, false);
        User challenger = challenge.getChallenger();
        User opponent = challenge.getOpponent();
        if (!friendService.areFriends(challenger.getId(), opponent.getId())) {
            challenge.resolve(ChallengeStatus.CANCELLED, Instant.now(clock));
            throw new ChallengeCancelledException();
        }
        DeckCheck opponentDeck = deckService.checkOwnDeck(userId, request.deckId());
        if (!opponentDeck.isValid()) {
            throw new InvalidDeckException(opponentDeck.problems());
        }
        DeckCheck challengerDeck = challengerDeckCheck(challenge);
        if (challengerDeck == null || !challengerDeck.isValid()) {
            throw new ChallengerDeckInvalidException(challenger.getUsername(),
                    challengerDeck == null ? List.of(DELETED_DECK_PROBLEM) : challengerDeck.problems());
        }
        Match match = matchService.play(MatchType.CASUAL, challenger, challengerDeck, opponent, opponentDeck,
                challenge);
        challenge.complete(opponentDeck.deck(), Instant.now(clock));
        return new AcceptChallengeResponse(match.getId());
    }

    /**
     * Rifiuta una sfida ricevuta ({@code POST /challenges/{challengeId}/reject}).
     *
     * @throws ChallengeNotFoundException         se la sfida non esiste o non riguarda l'utente
     * @throws ChallengeActionNotAllowedException se l'utente è chi ha lanciato la sfida
     * @throws ChallengeNotPendingException       se la sfida è già chiusa
     */
    @Transactional
    public void rejectChallenge(UUID userId, UUID challengeId) {
        userService.getActiveUser(userId);
        pendingChallengeForUpdate(userId, challengeId, false).resolve(ChallengeStatus.REJECTED, Instant.now(clock));
    }

    /**
     * Annulla una sfida lanciata ({@code POST /challenges/{challengeId}/cancel}).
     *
     * @throws ChallengeNotFoundException         se la sfida non esiste o non riguarda l'utente
     * @throws ChallengeActionNotAllowedException se l'utente è chi è stato sfidato
     * @throws ChallengeNotPendingException       se la sfida è già chiusa
     */
    @Transactional
    public void cancelChallenge(UUID userId, UUID challengeId) {
        userService.getActiveUser(userId);
        pendingChallengeForUpdate(userId, challengeId, true).resolve(ChallengeStatus.CANCELLED, Instant.now(clock));
    }

    /**
     * Il contatore delle sfide nel menu e nella Dashboard: quelle ricevute che aspettano una risposta, più quelle
     * lanciate con un mazzo da sistemare.
     *
     * @param userId l'utente collegato
     * @return quante sfide chiedono qualcosa all'utente
     */
    @Transactional(readOnly = true)
    public int countNeedingAttention(UUID userId) {
        int received = challengeRepository.countByOpponent_IdAndStatus(userId, ChallengeStatus.PENDING);
        long sentWithInvalidDeck = challengeRepository
                .findByChallenger_IdAndStatusOrderByCreatedAtDesc(userId, ChallengeStatus.PENDING).stream()
                .map(this::challengerDeckCheck)
                .filter(deck -> deck == null || !deck.isValid())
                .count();
        return received + (int) sentWithInvalidDeck;
    }

    // Due giocatori non sono più amici (uno ha tolto l'altro o ha cancellato l'account): le sfide tra loro si
    // annullano (decisione 026)
    @EventListener
    @Transactional
    public void onFriendshipRemoved(FriendshipRemovedEvent event) {
        Instant now = Instant.now(clock);
        challengeRepository.findPendingBetween(event.userId(), event.formerFriendId())
                .forEach(challenge -> challenge.resolve(ChallengeStatus.CANCELLED, now));
    }

    @EventListener
    @Transactional
    public void onAccountDeleted(AccountDeletedEvent event) {
        Instant now = Instant.now(clock);
        challengeRepository.findPendingOf(event.userId())
                .forEach(challenge -> challenge.resolve(ChallengeStatus.CANCELLED, now));
    }

    // La sfida bloccata, in attesa e con l'utente dal lato giusto: chi ha sfidato annulla, chi è sfidato decide
    private Challenge pendingChallengeForUpdate(UUID userId, UUID challengeId, boolean asChallenger) {
        Challenge challenge = challengeRepository.findByIdForUpdate(challengeId)
                .filter(found -> found.involves(userId))
                .orElseThrow(ChallengeNotFoundException::new);
        boolean isChallenger = challenge.getChallenger().getId().equals(userId);
        if (isChallenger != asChallenger) {
            throw new ChallengeActionNotAllowedException();
        }
        if (!challenge.isPending()) {
            throw new ChallengeNotPendingException();
        }
        return challenge;
    }

    // Il mazzo di chi ha sfidato, controllato adesso; null se è stato cancellato
    private DeckCheck challengerDeckCheck(Challenge challenge) {
        return challenge.getChallengerDeck() == null ? null : deckService.checkDeck(challenge.getChallengerDeck());
    }

    private ChallengeResponse toResponse(Challenge challenge) {
        return toResponse(challenge, challengerDeckCheck(challenge));
    }

    private static ChallengeResponse toResponse(Challenge challenge, DeckCheck deck) {
        List<String> problems = deck == null ? List.of(DELETED_DECK_PROBLEM) : deck.problems();
        return new ChallengeResponse(
                challenge.getId(),
                UserSummaryResponse.from(challenge.getChallenger()),
                UserSummaryResponse.from(challenge.getOpponent()),
                deck != null ? deck.deck().getId() : null,
                deck != null ? deck.deck().getName() : null,
                problems.isEmpty(),
                problems,
                challenge.getCreatedAt());
    }
}
