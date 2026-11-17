package it.somnia.dreamscape.api.friend.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.friend.dto.response.FriendRequestResponse;
import it.somnia.dreamscape.api.friend.dto.response.FriendResponse;
import it.somnia.dreamscape.api.friend.model.Friendship;
import it.somnia.dreamscape.api.friend.model.FriendshipStatus;
import it.somnia.dreamscape.api.friend.repository.FriendshipRepository;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.AlreadyFriendsException;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.CannotFriendYourselfException;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.FriendNotFoundException;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.FriendRequestAlreadySentException;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.FriendRequestNotFoundException;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.UserNotFoundException;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.AccountDeletedEvent;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * La lista amici (decisione 009, regole in docs/database/schema.md, sezione Amici): ricerca dei giocatori,
 * richieste, accettazione, rifiuto e rimozione. Nessun limite di amici né di richieste.
 *
 * <p>Quando un'amicizia finisce pubblica {@link FriendshipRemovedEvent}: scambi e sfide in attesa tra i due
 * si annullano nella stessa transazione (decisione 026).
 */
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Il risultato di {@code POST /friend-requests}: una richiesta nuova, oppure un'amicizia subito accettata
     * perché l'altro giocatore aveva già mandato una richiesta. Uno dei due è sempre {@code null}.
     */
    public record SendResult(FriendRequestResponse request, FriendResponse friend) {

        public boolean isAccepted() {
            return friend != null;
        }
    }

    /**
     * Restituisce gli amici dell'utente, in ordine di username.
     *
     * @param userId l'utente collegato
     * @return gli amici con la data dell'amicizia
     */
    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(UUID userId) {
        userService.getActiveUser(userId);
        return friendshipRepository.findAllOfUser(userId, FriendshipStatus.ACCEPTED).stream()
                .map(friendship -> toFriend(friendship, userId))
                .sorted(Comparator.comparing(friend -> friend.user().username().toLowerCase()))
                .toList();
    }

    /**
     * Cerca i giocatori per parte dello username, senza distinguere maiuscole e minuscole (al massimo 20).
     * Non restituisce chi cerca, gli admin e gli account cancellati.
     *
     * @param userId l'utente collegato
     * @param search parte dello username
     * @return id e username dei giocatori trovati, in ordine di username
     */
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> searchUsers(UUID userId, String search) {
        userService.getActiveUser(userId);
        return userRepository
                .findTop20ByEnabledTrueAndRoleAndUsernameContainingIgnoreCaseAndIdNotOrderByUsernameAsc(
                        UserRole.PLAYER, search.strip(), userId)
                .stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    /**
     * Manda una richiesta di amicizia. Se l'altro giocatore aveva già chiesto l'amicizia, la accetta subito.
     *
     * @param userId   l'utente collegato
     * @param username lo username del giocatore, senza distinguere maiuscole e minuscole
     * @return la richiesta nuova, oppure l'amico se l'amicizia è stata accettata subito
     * @throws UserNotFoundException             se non c'è un giocatore attivo con quello username
     * @throws CannotFriendYourselfException     se lo username è il proprio
     * @throws AlreadyFriendsException           se siete già amici
     * @throws FriendRequestAlreadySentException se c'è già una richiesta in attesa mandata dall'utente
     */
    @Transactional
    public SendResult sendRequest(UUID userId, String username) {
        User user = userService.getActiveUser(userId);
        User other = userRepository.findByUsernameIgnoreCase(username.strip())
                .filter(found -> found.isEnabled() && found.getRole() == UserRole.PLAYER)
                .orElseThrow(() -> new UserNotFoundException("username"));
        if (other.getId().equals(userId)) {
            throw new CannotFriendYourselfException();
        }
        Optional<Friendship> existing = friendshipRepository.findBetween(userId, other.getId());
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            if (friendship.isAccepted()) {
                throw new AlreadyFriendsException();
            }
            if (friendship.getRequester().getId().equals(userId)) {
                throw new FriendRequestAlreadySentException();
            }
            // L'altro aveva già chiesto l'amicizia: la nuova richiesta vale come risposta
            friendship.accept(Instant.now(clock));
            return new SendResult(null, toFriend(friendship, userId));
        }
        // saveAndFlush: la data della richiesta la scrive Hibernate all'inserimento e serve nella risposta
        Friendship request = friendshipRepository.saveAndFlush(new Friendship(user, other));
        return new SendResult(toRequest(request, userId), null);
    }

    /**
     * Restituisce le richieste in attesa ricevute o mandate dall'utente, dalla più recente.
     *
     * @param userId   l'utente collegato
     * @param received {@code true} per le richieste ricevute, {@code false} per quelle mandate
     * @return le richieste, con l'altro giocatore
     */
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listRequests(UUID userId, boolean received) {
        userService.getActiveUser(userId);
        List<Friendship> requests = received
                ? friendshipRepository.findReceivedRequests(userId)
                : friendshipRepository.findSentRequests(userId);
        return requests.stream().map(request -> toRequest(request, userId)).toList();
    }

    /**
     * Accetta una richiesta ricevuta.
     *
     * @param userId    l'utente collegato, destinatario della richiesta
     * @param requestId l'id della richiesta
     * @return il nuovo amico
     * @throws FriendRequestNotFoundException se la richiesta non esiste, non è in attesa o non è per l'utente
     */
    @Transactional
    public FriendResponse acceptRequest(UUID userId, UUID requestId) {
        userService.getActiveUser(userId);
        Friendship request = friendshipRepository.findById(requestId)
                .filter(found -> !found.isAccepted() && found.getAddressee().getId().equals(userId))
                .orElseThrow(FriendRequestNotFoundException::new);
        request.accept(Instant.now(clock));
        return toFriend(request, userId);
    }

    /**
     * Rifiuta una richiesta ricevuta o annulla una richiesta mandata: la riga si cancella.
     *
     * @param userId    l'utente collegato, destinatario o mittente della richiesta
     * @param requestId l'id della richiesta
     * @throws FriendRequestNotFoundException se la richiesta non esiste, non è in attesa o non riguarda l'utente
     */
    @Transactional
    public void deleteRequest(UUID userId, UUID requestId) {
        userService.getActiveUser(userId);
        Friendship request = friendshipRepository.findById(requestId)
                .filter(found -> !found.isAccepted() && involves(found, userId))
                .orElseThrow(FriendRequestNotFoundException::new);
        friendshipRepository.delete(request);
    }

    /**
     * Toglie un amico. Nella stessa transazione scambi e sfide in attesa tra i due si annullano
     * ({@link FriendshipRemovedEvent}).
     *
     * @param userId   l'utente collegato
     * @param friendId l'amico da togliere
     * @throws FriendNotFoundException se i due non sono amici
     */
    @Transactional
    public void removeFriend(UUID userId, UUID friendId) {
        userService.getActiveUser(userId);
        Friendship friendship = friendshipRepository.findBetween(userId, friendId)
                .filter(Friendship::isAccepted)
                .orElseThrow(FriendNotFoundException::new);
        friendshipRepository.delete(friendship);
        eventPublisher.publishEvent(new FriendshipRemovedEvent(userId, friendId));
    }

    /**
     * Dice se due giocatori sono amici (amicizia accettata). Lo usano scambi e collezione degli amici.
     *
     * @param userId  un giocatore
     * @param otherId l'altro giocatore
     * @return {@code true} se l'amicizia è accettata
     */
    @Transactional(readOnly = true)
    public boolean areFriends(UUID userId, UUID otherId) {
        return friendshipRepository.findBetween(userId, otherId).filter(Friendship::isAccepted).isPresent();
    }

    /**
     * Il contatore delle richieste ricevute in attesa, per la Dashboard.
     *
     * @param userId l'utente collegato
     * @return quante richieste aspettano una risposta dell'utente
     */
    @Transactional(readOnly = true)
    public int countPendingReceived(UUID userId) {
        return friendshipRepository.countByAddressee_IdAndStatus(userId, FriendshipStatus.PENDING);
    }

    /**
     * Account cancellato: si tolgono tutte le sue amicizie e richieste. Per ogni amicizia finita si pubblica
     * {@link FriendshipRemovedEvent}, così scambi e sfide in attesa si annullano nella stessa transazione.
     *
     * @param event l'account cancellato
     */
    @EventListener
    @Transactional
    public void onAccountDeleted(AccountDeletedEvent event) {
        UUID userId = event.userId();
        for (Friendship friendship : friendshipRepository.findAllOfUser(userId)) {
            friendshipRepository.delete(friendship);
            if (friendship.isAccepted()) {
                eventPublisher.publishEvent(
                        new FriendshipRemovedEvent(userId, friendship.otherThan(userId).getId()));
            }
        }
    }

    private static boolean involves(Friendship friendship, UUID userId) {
        return friendship.getRequester().getId().equals(userId) || friendship.getAddressee().getId().equals(userId);
    }

    private static FriendResponse toFriend(Friendship friendship, UUID userId) {
        return new FriendResponse(UserSummaryResponse.from(friendship.otherThan(userId)), friendship.getAcceptedAt());
    }

    private static FriendRequestResponse toRequest(Friendship friendship, UUID userId) {
        return new FriendRequestResponse(friendship.getId(), UserSummaryResponse.from(friendship.otherThan(userId)),
                friendship.getCreatedAt());
    }
}
