package it.somnia.dreamscape.api.trade.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.card.service.CardExceptions.CardNotFoundException;
import it.somnia.dreamscape.api.card.service.CardMapper;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.deck.dto.response.CollectionCardResponse;
import it.somnia.dreamscape.api.deck.model.DeckCards;
import it.somnia.dreamscape.api.deck.model.UserCards;
import it.somnia.dreamscape.api.deck.repository.DeckCardsRepository;
import it.somnia.dreamscape.api.deck.repository.UserCardsRepository;
import it.somnia.dreamscape.api.deck.service.CollectionService;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.NotFriendsException;
import it.somnia.dreamscape.api.friend.service.FriendExceptions.UserNotFoundException;
import it.somnia.dreamscape.api.friend.service.FriendService;
import it.somnia.dreamscape.api.friend.service.FriendshipRemovedEvent;
import it.somnia.dreamscape.api.trade.dto.request.TradeCardRequest;
import it.somnia.dreamscape.api.trade.dto.request.TradeRequest;
import it.somnia.dreamscape.api.trade.dto.response.TradeCardResponse;
import it.somnia.dreamscape.api.trade.dto.response.TradeResponse;
import it.somnia.dreamscape.api.trade.dto.response.TradeWarningsResponse;
import it.somnia.dreamscape.api.trade.model.Trade;
import it.somnia.dreamscape.api.trade.model.TradeItem;
import it.somnia.dreamscape.api.trade.model.TradeSide;
import it.somnia.dreamscape.api.trade.model.TradeStatus;
import it.somnia.dreamscape.api.trade.repository.TradeItemRepository;
import it.somnia.dreamscape.api.trade.repository.TradeRepository;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.CardsLockedInTradeException;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.CardsNotOwnedException;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.InvalidTradeException;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.RequestedCardsNotAvailableException;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.TradeActionNotAllowedException;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.TradeCancelledException;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.TradeNotFoundException;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.TradeNotPendingException;
import it.somnia.dreamscape.api.user.dto.response.UserSummaryResponse;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;
import it.somnia.dreamscape.api.user.repository.UserRepository;
import it.somnia.dreamscape.api.user.service.AccountDeletedEvent;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Gli scambi tra amici (decisioni 009 e 024, regole in docs/database/schema.md, sezione Scambi): proposta,
 * liste, dettaglio, rifiuto e annullamento, e la collezione di un amico per scegliere le carte.
 *
 * <p>Le copie offerte in uno scambio in attesa sono bloccate per gli altri scambi di chi propone
 * ({@link LockedCopies}). Alla proposta le righe della collezione di chi propone si bloccano
 * ({@code SELECT ... FOR UPDATE}): due proposte nello stesso momento con la stessa copia vanno una alla volta
 * e la seconda vede la prima. Le copie richieste all'amico non si bloccano: si controlla solo che ci siano.
 */
@Service
@RequiredArgsConstructor
public class TradeService {

    private static final String BOX_RECEIVED = "received";
    private static final String BOX_SENT = "sent";

    private final TradeRepository tradeRepository;
    private final TradeItemRepository tradeItemRepository;
    private final UserCardsRepository userCardsRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final FriendService friendService;
    private final CollectionService collectionService;
    private final LockedCopies lockedCopies;
    private final TradeImpact tradeImpact;
    private final DeckCardsRepository deckCardsRepository;
    private final CardMapper cardMapper;
    private final Clock clock;

    /**
     * Propone uno scambio a un amico.
     *
     * @param userId  chi propone, l'utente collegato
     * @param request l'amico, il messaggio e le carte dei due lati (la stessa carta ripetuta si somma)
     * @return lo scambio in attesa
     * @throws InvalidTradeException               se lo scambio è verso se stessi o una carta è su tutti e due i lati
     * @throws UserNotFoundException               se l'amico non esiste o ha cancellato l'account
     * @throws NotFriendsException                 se i due non sono amici
     * @throws CardNotFoundException               se una carta non esiste
     * @throws CardsNotOwnedException              se chi propone non possiede abbastanza copie di una carta offerta
     * @throws CardsLockedInTradeException         se le copie ci sono, ma sono già offerte in altri scambi
     * @throws RequestedCardsNotAvailableException se l'amico non ha abbastanza copie disponibili di una carta richiesta
     */
    @Transactional
    public TradeResponse proposeTrade(UUID userId, TradeRequest request) {
        Proposal proposal = prepare(userId, request, true);
        String message = request.message() == null || request.message().isBlank() ? null : request.message().strip();
        Trade trade = new Trade(proposal.proposer(), proposal.recipient(), message);
        Map<UUID, Card> cards = proposal.cards();
        sortedByCode(proposal.offered(), cards).forEach(cardId -> trade.addItem(cards.get(cardId), TradeSide.OFFERED,
                proposal.offered().get(cardId)));
        sortedByCode(proposal.requested(), cards).forEach(cardId -> trade.addItem(cards.get(cardId),
                TradeSide.REQUESTED, proposal.requested().get(cardId)));
        // saveAndFlush: la data della proposta la scrive Hibernate all'inserimento e serve nella risposta
        return toResponse(tradeRepository.saveAndFlush(trade));
    }

    /**
     * Gli avvisi prima di proporre ({@code POST /trades/preview}): mazzi di chi propone che cambiano e carte offerte
     * che sono l'ultima copia. Fa gli stessi controlli della proposta, ma non salva niente.
     *
     * @param userId  chi propone, l'utente collegato
     * @param request la proposta, come per {@code POST /trades}
     * @return gli avvisi; liste vuote se non c'è niente da dire
     * @throws InvalidTradeException e gli altri errori della proposta, alle stesse condizioni
     */
    @Transactional(readOnly = true)
    public TradeWarningsResponse previewTrade(UUID userId, TradeRequest request) {
        Proposal proposal = prepare(userId, request, false);
        TradeImpact.Impact impact = tradeImpact.of(proposal.proposer(), proposal.offered(), proposal.requested(),
                proposal.cards());
        return new TradeWarningsResponse(impact.deckWarnings(), impact.lastCopyWarnings());
    }

    /**
     * Gli avvisi di uno scambio in attesa per chi lo guarda ({@code GET /trades/{tradeId}/deck-warnings}): per chi
     * riceve sono le carte che darebbe accettando, per chi propone quelle che ha offerto.
     *
     * @throws TradeNotFoundException   se lo scambio non esiste o non riguarda l'utente
     * @throws TradeNotPendingException se lo scambio è già chiuso
     */
    @Transactional(readOnly = true)
    public TradeWarningsResponse getTradeWarnings(UUID userId, UUID tradeId) {
        User user = userService.getActiveUser(userId);
        Trade trade = tradeRepository.findById(tradeId)
                .filter(found -> found.involves(userId))
                .orElseThrow(TradeNotFoundException::new);
        if (!trade.isPending()) {
            throw new TradeNotPendingException();
        }
        boolean isRecipient = trade.getRecipient().getId().equals(userId);
        TradeImpact.Impact impact = tradeImpact.of(user,
                quantities(trade, isRecipient ? TradeSide.REQUESTED : TradeSide.OFFERED),
                quantities(trade, isRecipient ? TradeSide.OFFERED : TradeSide.REQUESTED), cardsOf(trade));
        return new TradeWarningsResponse(impact.deckWarnings(), impact.lastCopyWarnings());
    }

    /**
     * Accetta uno scambio ricevuto e sposta le carte, tutto nella stessa transazione (schema.md, sezione Scambi).
     *
     * <ol>
     *   <li>blocca lo scambio; se i due non sono più amici lo annulla ({@code 409 TRADE_CANCELLED});</li>
     *   <li>blocca le righe della collezione di tutti e due, sempre nello stesso ordine;</li>
     *   <li>ricontrolla le copie: se mancano risponde {@code 409} e lo scambio <b>resta in attesa</b>;</li>
     *   <li>sposta le carte e passa alla versione normale i mazzi che usavano una full art ceduta, se ce n'è
     *       una copia libera;</li>
     *   <li>chiude lo scambio come {@code ACCEPTED}.</li>
     * </ol>
     *
     * @throws TradeNotFoundException              se lo scambio non esiste o non riguarda l'utente
     * @throws TradeActionNotAllowedException      se l'utente è chi propone
     * @throws TradeNotPendingException            se lo scambio è già chiuso
     * @throws TradeCancelledException             se i due non sono più amici: lo scambio è stato annullato
     * @throws RequestedCardsNotAvailableException se l'utente non ha più abbastanza copie disponibili
     * @throws CardsNotOwnedException              se chi propone non ha più le copie offerte
     */
    @Transactional(noRollbackFor = TradeCancelledException.class)
    public TradeResponse acceptTrade(UUID userId, UUID tradeId) {
        userService.getActiveUser(userId);
        Trade trade = pendingTradeForUpdate(userId, tradeId, TradeSide.REQUESTED);
        User proposer = trade.getProposer();
        User recipient = trade.getRecipient();
        Instant now = Instant.now(clock);
        if (!friendService.areFriends(proposer.getId(), recipient.getId())) {
            trade.resolve(TradeStatus.CANCELLED, now);
            throw new TradeCancelledException();
        }
        Map<UUID, Integer> offered = quantities(trade, TradeSide.OFFERED);
        Map<UUID, Integer> requested = quantities(trade, TradeSide.REQUESTED);
        Map<UUID, Card> cards = cardsOf(trade);

        // Righe di tutti e due per tutte le carte dello scambio: chi dà e chi riceve. Prima l'utente con l'id
        // più basso, così due accettazioni tra gli stessi giocatori non si aspettano a vicenda
        Set<UUID> allCards = new HashSet<>(cards.keySet());
        Map<UUID, UserCards> proposerRows;
        Map<UUID, UserCards> recipientRows;
        if (proposer.getId().compareTo(recipient.getId()) < 0) {
            proposerRows = lockRows(proposer.getId(), allCards);
            recipientRows = lockRows(recipient.getId(), allCards);
        } else {
            recipientRows = lockRows(recipient.getId(), allCards);
            proposerRows = lockRows(proposer.getId(), allCards);
        }

        checkStillAvailable(trade, proposer, offered, proposerRows, recipient, requested, recipientRows, cards);

        // Gli effetti sui mazzi si calcolano prima di spostare le carte, con lo stesso calcolo degli avvisi
        TradeImpact.Impact proposerImpact = tradeImpact.of(proposer, offered, requested, cards);
        TradeImpact.Impact recipientImpact = tradeImpact.of(recipient, requested, offered, cards);

        offered.forEach((cardId, copies) -> move(cards.get(cardId), copies, proposerRows, recipient, recipientRows));
        requested.forEach((cardId, copies) -> move(cards.get(cardId), copies, recipientRows, proposer, proposerRows));
        applySwaps(proposerImpact.swaps());
        applySwaps(recipientImpact.swaps());

        trade.resolve(TradeStatus.ACCEPTED, now);
        return toResponse(trade);
    }

    /**
     * Restituisce una pagina degli scambi dell'utente.
     *
     * @param userId l'utente collegato
     * @param box    {@code received} (in attesa di una mia risposta), {@code sent} (proposti da me, in attesa)
     *               o {@code history} (chiusi, da entrambi i lati)
     * @param page   numero della pagina, da 0
     * @param size   scambi per pagina
     * @return la pagina, dal più recente
     */
    @Transactional(readOnly = true)
    public PageResponse<TradeResponse> listTrades(UUID userId, String box, int page, int size) {
        userService.getActiveUser(userId);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Trade> trades = switch (box) {
            case BOX_RECEIVED -> tradeRepository.findByRecipient_IdAndStatusOrderByCreatedAtDesc(userId,
                    TradeStatus.PENDING, pageRequest);
            case BOX_SENT -> tradeRepository.findByProposer_IdAndStatusOrderByCreatedAtDesc(userId,
                    TradeStatus.PENDING, pageRequest);
            default -> tradeRepository.findHistory(userId, pageRequest);
        };
        List<TradeResponse> items = toResponses(trades.getContent());
        return new PageResponse<>(items, trades.getNumber(), trades.getSize(), trades.getTotalElements(),
                trades.getTotalPages());
    }

    /**
     * Restituisce uno scambio dell'utente, come proponente o come destinatario.
     *
     * @throws TradeNotFoundException se lo scambio non esiste o non riguarda l'utente
     */
    @Transactional(readOnly = true)
    public TradeResponse getTrade(UUID userId, UUID tradeId) {
        userService.getActiveUser(userId);
        Trade trade = tradeRepository.findById(tradeId)
                .filter(found -> found.involves(userId))
                .orElseThrow(TradeNotFoundException::new);
        return toResponse(trade);
    }

    /**
     * Rifiuta uno scambio ricevuto: le copie offerte da chi propone tornano disponibili.
     *
     * @throws TradeNotFoundException          se lo scambio non esiste o non riguarda l'utente
     * @throws TradeActionNotAllowedException  se l'utente è chi propone (lui annulla, non rifiuta)
     * @throws TradeNotPendingException        se lo scambio è già chiuso
     */
    @Transactional
    public TradeResponse rejectTrade(UUID userId, UUID tradeId) {
        userService.getActiveUser(userId);
        Trade trade = pendingTradeForUpdate(userId, tradeId, TradeSide.REQUESTED);
        trade.resolve(TradeStatus.REJECTED, Instant.now(clock));
        return toResponse(trade);
    }

    /**
     * Annulla uno scambio proposto: le copie offerte tornano disponibili.
     *
     * @throws TradeNotFoundException          se lo scambio non esiste o non riguarda l'utente
     * @throws TradeActionNotAllowedException  se l'utente è chi riceve (lui rifiuta, non annulla)
     * @throws TradeNotPendingException        se lo scambio è già chiuso
     */
    @Transactional
    public TradeResponse cancelTrade(UUID userId, UUID tradeId) {
        userService.getActiveUser(userId);
        Trade trade = pendingTradeForUpdate(userId, tradeId, TradeSide.OFFERED);
        trade.resolve(TradeStatus.CANCELLED, Instant.now(clock));
        return toResponse(trade);
    }

    /**
     * La collezione di un amico, per scegliere le carte da chiedere ({@code GET /users/{userId}/cards}), con gli
     * stessi filtri di {@code GET /me/cards}. {@code availableForTrade} sono le copie che l'amico non ha già
     * offerto in altri scambi.
     *
     * @throws UserNotFoundException se il giocatore non esiste o ha cancellato l'account
     * @throws NotFriendsException   se non è un amico
     */
    @Transactional(readOnly = true)
    public PageResponse<CollectionCardResponse> listFriendCollection(UUID userId, UUID friendId, String factionCode,
            String rarityCode, String search, Boolean fullArt, int page, int size) {
        userService.getActiveUser(userId);
        User friend = activePlayer(friendId);
        if (!friend.getId().equals(userId) && !friendService.areFriends(userId, friend.getId())) {
            throw new NotFriendsException();
        }
        return collectionService.listCollection(friend.getId(), factionCode, rarityCode, search, fullArt, page, size);
    }

    /**
     * Il contatore degli scambi ricevuti in attesa, per la Dashboard.
     */
    @Transactional(readOnly = true)
    public int countPendingReceived(UUID userId) {
        return tradeRepository.countByRecipient_IdAndStatus(userId, TradeStatus.PENDING);
    }

    /**
     * Amicizia tolta (anche per un account cancellato): gli scambi in attesa tra i due si annullano nella stessa
     * transazione e le copie offerte tornano disponibili (decisione 026).
     */
    @EventListener
    @Transactional
    public void onFriendshipRemoved(FriendshipRemovedEvent event) {
        Instant now = Instant.now(clock);
        tradeRepository.findPendingBetween(event.userId(), event.formerFriendId())
                .forEach(trade -> trade.resolve(TradeStatus.CANCELLED, now));
    }

    /**
     * Account cancellato: si annullano tutti i suoi scambi in attesa, da entrambi i lati.
     */
    @EventListener
    @Transactional
    public void onAccountDeleted(AccountDeletedEvent event) {
        Instant now = Instant.now(clock);
        tradeRepository.findPendingOf(event.userId()).forEach(trade -> trade.resolve(TradeStatus.CANCELLED, now));
    }

    // ------------------------------------------------------------------ controlli della proposta

    // Una proposta controllata: le copie con le carte sommate e le carte caricate
    private record Proposal(User proposer, User recipient, Map<UUID, Integer> offered, Map<UUID, Integer> requested,
            Map<UUID, Card> cards) {
    }

    // I controlli comuni a proposta e anteprima. lock = true blocca le righe di chi propone (solo la proposta)
    private Proposal prepare(UUID userId, TradeRequest request, boolean lock) {
        User proposer = userService.getActiveUser(userId);
        if (request.recipientId().equals(userId)) {
            throw new InvalidTradeException("Non puoi proporre uno scambio a te stesso.");
        }
        User recipient = activePlayer(request.recipientId());
        if (!friendService.areFriends(userId, recipient.getId())) {
            throw new NotFriendsException();
        }
        Map<UUID, Integer> offered = sumByCard(request.offered());
        Map<UUID, Integer> requested = sumByCard(request.requested());
        if (offered.keySet().stream().anyMatch(requested::containsKey)) {
            throw new InvalidTradeException("La stessa carta non può essere sia offerta sia richiesta.");
        }
        Map<UUID, Card> cards = loadCards(offered.keySet(), requested.keySet());
        checkOffered(userId, offered, cards, lock);
        checkRequested(recipient, requested, cards);
        return new Proposal(proposer, recipient, offered, requested, cards);
    }

    // Le copie offerte: le righe della collezione si bloccano prima di contare quelle già offerte, così una
    // seconda proposta nello stesso momento aspetta qui e poi vede lo scambio della prima
    private void checkOffered(UUID userId, Map<UUID, Integer> offered, Map<UUID, Card> cards, boolean lock) {
        Map<UUID, Integer> owned = lock
                ? userCardsRepository.findForUpdate(userId, offered.keySet()).stream()
                        .collect(Collectors.toMap(userCard -> userCard.getCard().getId(), UserCards::getQuantity))
                : collectionService.ownedQuantities(userId);
        Map<UUID, Integer> locked = lockedCopies.of(userId);
        for (UUID cardId : sortedByCode(offered, cards)) {
            int wanted = offered.get(cardId);
            int ownedCopies = owned.getOrDefault(cardId, 0);
            String label = label(cards.get(cardId));
            if (ownedCopies < wanted) {
                throw new CardsNotOwnedException(ownedCopies == 0
                        ? "Non possiedi la carta " + label + "."
                        : "Possiedi " + copies(ownedCopies) + " di " + label + ": ne offri " + wanted + ".");
            }
            int available = ownedCopies - locked.getOrDefault(cardId, 0);
            if (available < wanted) {
                throw new CardsLockedInTradeException(available <= 0
                        ? "Tutte le tue copie di " + label + " sono già offerte in scambi in attesa."
                        : "Puoi offrire al massimo " + copies(available) + " di " + label
                                + ": le altre sono già offerte in scambi in attesa.");
            }
        }
    }

    // Le copie richieste: l'amico deve averle disponibili oggi, ma non si bloccano (decisione 024)
    private void checkRequested(User recipient, Map<UUID, Integer> requested, Map<UUID, Card> cards) {
        Map<UUID, Integer> owned = collectionService.ownedQuantities(recipient.getId());
        Map<UUID, Integer> locked = lockedCopies.of(recipient.getId());
        for (UUID cardId : sortedByCode(requested, cards)) {
            int wanted = requested.get(cardId);
            int available = owned.getOrDefault(cardId, 0) - locked.getOrDefault(cardId, 0);
            if (available < wanted) {
                String label = label(cards.get(cardId));
                throw new RequestedCardsNotAvailableException(available <= 0
                        ? recipient.getUsername() + " non ha copie disponibili di " + label + "."
                        : recipient.getUsername() + " ha " + copies(available)
                                + (available == 1 ? " disponibile" : " disponibili") + " di " + label
                                + ": ne chiedi " + wanted + ".");
            }
        }
    }

    // ------------------------------------------------------------------ accettazione

    // Ricontrollo all'accettazione, con le righe già bloccate. Chi propone: le sue copie offerte sono bloccate
    // per gli altri scambi, quindi conta tutto tranne quelle offerte negli altri scambi. Chi riceve: contano
    // solo le copie disponibili, non quelle che ha offerto in altri scambi in attesa
    private void checkStillAvailable(Trade trade, User proposer, Map<UUID, Integer> offered,
            Map<UUID, UserCards> proposerRows, User recipient, Map<UUID, Integer> requested,
            Map<UUID, UserCards> recipientRows, Map<UUID, Card> cards) {
        Map<UUID, Integer> proposerLocked = lockedCopies.of(proposer.getId());
        for (UUID cardId : sortedByCode(offered, cards)) {
            int lockedByOthers = proposerLocked.getOrDefault(cardId, 0) - offered.get(cardId);
            int available = quantityOf(proposerRows, cardId) - lockedByOthers;
            if (available < offered.get(cardId)) {
                throw new CardsNotOwnedException(proposer.getUsername() + " non ha più " + copies(offered.get(cardId))
                        + " di " + label(cards.get(cardId)) + ".");
            }
        }
        Map<UUID, Integer> recipientLocked = lockedCopies.of(recipient.getId());
        for (UUID cardId : sortedByCode(requested, cards)) {
            int wanted = requested.get(cardId);
            int available = quantityOf(recipientRows, cardId) - recipientLocked.getOrDefault(cardId, 0);
            if (available < wanted) {
                String label = label(cards.get(cardId));
                throw new RequestedCardsNotAvailableException(available <= 0
                        ? "Non hai copie disponibili di " + label + "."
                        : "Hai " + copies(available) + (available == 1 ? " disponibile" : " disponibili") + " di "
                                + label + ": lo scambio ne chiede " + wanted + ".");
            }
        }
    }

    private Map<UUID, UserCards> lockRows(UUID userId, Set<UUID> cardIds) {
        return userCardsRepository.findForUpdate(userId, cardIds).stream()
                .collect(Collectors.toMap(row -> row.getCard().getId(), Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
    }

    // Sposta le copie: chi dà scala (e la riga sparisce a 0), chi riceve aumenta o ha una riga nuova
    private void move(Card card, int copies, Map<UUID, UserCards> fromRows, User to, Map<UUID, UserCards> toRows) {
        UserCards from = fromRows.get(card.getId());
        from.setQuantity(from.getQuantity() - copies);
        if (from.getQuantity() == 0) {
            userCardsRepository.delete(from);
            fromRows.remove(card.getId());
        }
        UserCards target = toRows.get(card.getId());
        if (target != null) {
            target.setQuantity(target.getQuantity() + copies);
        } else {
            toRows.put(card.getId(), userCardsRepository.save(new UserCards(to, card, copies)));
        }
    }

    // La full art ceduta lascia il posto alla carta normale nel mazzo (schema.md, "Full art che esce da un mazzo")
    private void applySwaps(List<TradeImpact.FullArtSwap> swaps) {
        for (TradeImpact.FullArtSwap swap : swaps) {
            DeckCards fullArt = swap.fullArtEntry();
            fullArt.setQuantity(fullArt.getQuantity() - swap.copies());
            if (fullArt.getQuantity() == 0) {
                deckCardsRepository.delete(fullArt);
            }
            deckCardsRepository.findAllByDeck_Id(swap.deck().getId()).stream()
                    .filter(entry -> entry.getCard().getId().equals(swap.normalCard().getId()))
                    .findFirst()
                    .ifPresentOrElse(entry -> entry.setQuantity(entry.getQuantity() + swap.copies()),
                            () -> deckCardsRepository.save(new DeckCards(swap.deck(), swap.normalCard(), swap.copies())));
        }
    }

    private static int quantityOf(Map<UUID, UserCards> rows, UUID cardId) {
        UserCards row = rows.get(cardId);
        return row != null ? row.getQuantity() : 0;
    }

    private static Map<UUID, Integer> quantities(Trade trade, TradeSide side) {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        trade.itemsOf(side).forEach(item -> quantities.put(item.getCard().getId(), item.getQuantity()));
        return quantities;
    }

    private static Map<UUID, Card> cardsOf(Trade trade) {
        Map<UUID, Card> cards = new LinkedHashMap<>();
        trade.getItems().forEach(item -> cards.put(item.getCard().getId(), item.getCard()));
        return cards;
    }

    private Trade pendingTradeForUpdate(UUID userId, UUID tradeId, TradeSide userSide) {
        Trade trade = tradeRepository.findByIdForUpdate(tradeId)
                .filter(found -> found.involves(userId))
                .orElseThrow(TradeNotFoundException::new);
        User expected = userSide == TradeSide.OFFERED ? trade.getProposer() : trade.getRecipient();
        if (!expected.getId().equals(userId)) {
            throw new TradeActionNotAllowedException();
        }
        if (!trade.isPending()) {
            throw new TradeNotPendingException();
        }
        return trade;
    }

    // Un giocatore attivo (non un admin, non un account cancellato)
    private User activePlayer(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.isEnabled() && user.getRole() == UserRole.PLAYER)
                .orElseThrow(UserNotFoundException::new);
    }

    private Map<UUID, Card> loadCards(Set<UUID> offeredIds, Set<UUID> requestedIds) {
        Set<UUID> ids = new HashSet<>(offeredIds);
        ids.addAll(requestedIds);
        Map<UUID, Card> cards = cardRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));
        if (cards.size() != ids.size()) {
            throw new CardNotFoundException();
        }
        return cards;
    }

    private static Map<UUID, Integer> sumByCard(List<TradeCardRequest> cards) {
        Map<UUID, Integer> sums = new LinkedHashMap<>();
        cards.forEach(card -> sums.merge(card.cardId(), card.quantity(), Integer::sum));
        return sums;
    }

    // Le carte in ordine di codice: messaggi e righe dello scambio sempre nello stesso ordine
    private static List<UUID> sortedByCode(Map<UUID, Integer> quantities, Map<UUID, Card> cards) {
        return quantities.keySet().stream()
                .sorted(Comparator.comparing(cardId -> cards.get(cardId).getCode()))
                .toList();
    }

    // ------------------------------------------------------------------ risposte

    private TradeResponse toResponse(Trade trade) {
        return toResponses(List.of(trade)).getFirst();
    }

    // Carte di tutti gli scambi con due query (righe ed effetti), non una per scambio
    private List<TradeResponse> toResponses(List<Trade> trades) {
        if (trades.isEmpty()) {
            return List.of();
        }
        List<TradeItem> items = tradeItemRepository.findWithCardByTradeIds(trades.stream().map(Trade::getId).toList());
        List<Card> cards = items.stream().map(TradeItem::getCard).distinct().toList();
        List<CardResponse> cardResponses = cardMapper.toResponses(cards);
        Map<UUID, CardResponse> cardById = new LinkedHashMap<>();
        for (int i = 0; i < cards.size(); i++) {
            cardById.put(cards.get(i).getId(), cardResponses.get(i));
        }
        Map<UUID, List<TradeItem>> itemsByTrade = items.stream()
                .collect(Collectors.groupingBy(item -> item.getTrade().getId()));
        List<TradeResponse> responses = new ArrayList<>();
        for (Trade trade : trades) {
            List<TradeItem> tradeItems = itemsByTrade.getOrDefault(trade.getId(), List.of());
            responses.add(new TradeResponse(trade.getId(), trade.getStatus(),
                    UserSummaryResponse.from(trade.getProposer()), UserSummaryResponse.from(trade.getRecipient()),
                    trade.getMessage(), cardsOf(tradeItems, TradeSide.OFFERED, cardById),
                    cardsOf(tradeItems, TradeSide.REQUESTED, cardById), trade.getCreatedAt(), trade.getResolvedAt()));
        }
        return responses;
    }

    private static List<TradeCardResponse> cardsOf(List<TradeItem> items, TradeSide side,
            Map<UUID, CardResponse> cardById) {
        return items.stream()
                .filter(item -> item.getSide() == side)
                .sorted(Comparator.comparing(item -> item.getCard().getCode()))
                .map(item -> new TradeCardResponse(cardById.get(item.getCard().getId()), item.getQuantity()))
                .toList();
    }

    // Il nome della carta per il giocatore; la full art ha il nome della sua carta base
    static String label(Card card) {
        Card base = card.getBaseCard() != null ? card.getBaseCard() : card;
        return card.getVariantType() == CardVariantType.FULL_ART ? base.getName() + " (full art)" : base.getName();
    }

    private static String copies(int count) {
        return count + (count == 1 ? " copia" : " copie");
    }
}
