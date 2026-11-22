package it.somnia.dreamscape.api.deck.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.card.service.CardMapper;
import it.somnia.dreamscape.api.deck.dto.request.DeckCardRequest;
import it.somnia.dreamscape.api.deck.dto.request.DeckRequest;
import it.somnia.dreamscape.api.deck.dto.response.DeckCardResponse;
import it.somnia.dreamscape.api.deck.dto.response.DeckResponse;
import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.model.DeckCards;
import it.somnia.dreamscape.api.deck.repository.DeckCardsRepository;
import it.somnia.dreamscape.api.deck.repository.DeckRepository;
import it.somnia.dreamscape.api.deck.service.DeckExceptions.DeckNameTakenException;
import it.somnia.dreamscape.api.deck.service.DeckExceptions.DeckNotFoundException;
import it.somnia.dreamscape.api.deck.service.DeckExceptions.InvalidDeckException;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * I mazzi dell'utente collegato: lista, dettaglio, creazione, modifica ed eliminazione.
 *
 * <p>Si salvano solo mazzi validi (decisione 017): le regole sono in {@link DeckValidator}. Alla lettura lo stesso
 * controllo dice se un mazzo salvato è ancora valido e perché no ({@code isValid}, {@code problems}).
 * Il mazzo classificato ({@code isRanked}) è quello con cui si gioca quando il matchmaking ti sceglie come
 * avversario (#127).
 */
@Service
@RequiredArgsConstructor
public class DeckService {

    // Problema per un id di carta che non esiste nel catalogo
    static final String UNKNOWN_CARD_PROBLEM = "Una delle carte scelte non esiste.";

    private final DeckRepository deckRepository;
    private final DeckCardsRepository deckCardsRepository;
    private final CardRepository cardRepository;
    private final CollectionService collectionService;
    private final DeckValidator deckValidator;
    private final CardMapper cardMapper;
    private final UserService userService;

    /**
     * Restituisce i mazzi dell'utente ({@code GET /decks}), in ordine di nome, ognuno con le sue carte.
     *
     * @param userId l'utente collegato
     * @return i mazzi, lista vuota se non ne ha
     */
    @Transactional(readOnly = true)
    public List<DeckResponse> listDecks(UUID userId) {
        User user = userService.getActiveUser(userId);
        List<Deck> decks = deckRepository.findAllByUser_IdOrderByNameAsc(userId);
        if (decks.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<DeckCards>> cardsByDeckId = deckCardsRepository
                .findAllByDeck_IdIn(decks.stream().map(Deck::getId).toList()).stream()
                .collect(Collectors.groupingBy(deckCard -> deckCard.getDeck().getId()));
        Map<UUID, Integer> owned = collectionService.ownedQuantities(userId);
        Map<UUID, CardResponse> cardResponses = toCardResponses(cardsByDeckId.values().stream()
                .flatMap(List::stream).map(DeckCards::getCard).toList());
        return decks.stream()
                .map(deck -> toResponse(deck, cardsByDeckId.getOrDefault(deck.getId(), List.of()), owned,
                        rankedDeckId(user), cardResponses))
                .toList();
    }

    /**
     * Restituisce un mazzo dell'utente ({@code GET /decks/{deckId}}).
     *
     * @param userId l'utente collegato
     * @param deckId il mazzo
     * @return il mazzo con le carte, la validità e i problemi
     * @throws DeckNotFoundException se il mazzo non esiste o è di un altro utente
     */
    @Transactional(readOnly = true)
    public DeckResponse getDeck(UUID userId, UUID deckId) {
        User user = userService.getActiveUser(userId);
        Deck deck = deckRepository.findByIdAndUser_Id(deckId, userId).orElseThrow(DeckNotFoundException::new);
        return toResponse(deck, user);
    }

    /**
     * Crea un mazzo ({@code POST /decks}), solo se rispetta tutte le regole.
     *
     * <p>Se la stessa carta compare più volte nella richiesta, le copie si sommano.
     *
     * @param userId  l'utente collegato
     * @param request nome e carte del mazzo
     * @return il mazzo salvato
     * @throws DeckNameTakenException se l'utente ha già un mazzo con questo nome
     * @throws InvalidDeckException   se il mazzo non rispetta le regole, con tutti i problemi
     */
    @Transactional
    public DeckResponse createDeck(UUID userId, DeckRequest request) {
        User user = userService.getActiveUser(userId);
        String name = request.name().strip();
        if (deckRepository.existsByUser_IdAndName(userId, name)) {
            throw new DeckNameTakenException();
        }
        Map<Card, Integer> cards = validCards(userId, request);
        Deck deck = deckRepository.save(new Deck(user, name));
        saveCards(deck, cards);
        return toResponse(deck, user);
    }

    /**
     * Modifica nome e carte di un mazzo ({@code PUT /decks/{deckId}}), solo se il risultato rispetta tutte le regole.
     *
     * <p>Le carte della richiesta sostituiscono quelle di prima. Un mazzo diventato non valido si sistema così.
     *
     * @param userId  l'utente collegato
     * @param deckId  il mazzo da modificare
     * @param request il nuovo nome e le nuove carte
     * @return il mazzo salvato
     * @throws DeckNotFoundException  se il mazzo non esiste o è di un altro utente
     * @throws DeckNameTakenException se l'utente ha già un altro mazzo con questo nome
     * @throws InvalidDeckException   se il mazzo non rispetta le regole, con tutti i problemi
     */
    @Transactional
    public DeckResponse updateDeck(UUID userId, UUID deckId, DeckRequest request) {
        User user = userService.getActiveUser(userId);
        Deck deck = deckRepository.findByIdAndUser_Id(deckId, userId).orElseThrow(DeckNotFoundException::new);
        String name = request.name().strip();
        if (deckRepository.existsByUser_IdAndNameAndIdNot(userId, name, deckId)) {
            throw new DeckNameTakenException();
        }
        Map<Card, Integer> cards = validCards(userId, request);
        deck.setName(name);
        deckCardsRepository.deleteAllCardsOf(deck);
        saveCards(deck, cards);
        return toResponse(deck, user);
    }

    /**
     * Elimina un mazzo ({@code DELETE /decks/{deckId}}).
     *
     * <p>Le sue carte si cancellano con il mazzo (ON DELETE CASCADE). Se era il mazzo classificato, l'utente
     * resta senza (ON DELETE SET NULL) e il matchmaking non lo sceglie finché non ne sceglie un altro.
     *
     * @param userId l'utente collegato
     * @param deckId il mazzo da eliminare
     * @throws DeckNotFoundException se il mazzo non esiste o è di un altro utente
     */
    @Transactional
    public void deleteDeck(UUID userId, UUID deckId) {
        userService.getActiveUser(userId);
        Deck deck = deckRepository.findByIdAndUser_Id(deckId, userId).orElseThrow(DeckNotFoundException::new);
        deckRepository.deleteDeckById(deck.getId());
    }

    /**
     * Sceglie o toglie il mazzo classificato ({@code PUT /me/ranked-deck}).
     *
     * <p>Si sceglie solo un mazzo valido. Se poi diventa non valido (scambio, carta disattivata) resta il mazzo
     * classificato, ma il matchmaking non sceglie il giocatore come avversario finché non lo sistema.
     *
     * @param userId l'utente collegato
     * @param deckId un mazzo dell'utente; null per togliere il mazzo classificato
     * @throws DeckNotFoundException se il mazzo non esiste o è di un altro utente
     * @throws InvalidDeckException  se il mazzo non è valido, con tutti i problemi
     */
    @Transactional
    public void setRankedDeck(UUID userId, UUID deckId) {
        User user = userService.getActiveUser(userId);
        if (deckId == null) {
            user.setRankedDeck(null);
            return;
        }
        DeckCheck deck = checkOwnDeck(userId, deckId);
        if (!deck.isValid()) {
            throw new InvalidDeckException(deck.problems());
        }
        user.setRankedDeck(deck.deck());
    }

    /**
     * Controlla un mazzo dell'utente prima di giocarlo (sfide tra amici, #94; matchmaking, #127).
     *
     * @param userId l'utente collegato
     * @param deckId il mazzo scelto
     * @return il mazzo con le carte e i problemi di adesso
     * @throws DeckNotFoundException se il mazzo non esiste o è di un altro utente
     */
    @Transactional(readOnly = true)
    public DeckCheck checkOwnDeck(UUID userId, UUID deckId) {
        Deck deck = deckRepository.findByIdAndUser_Id(deckId, userId).orElseThrow(DeckNotFoundException::new);
        return checkDeck(deck);
    }

    /**
     * Controlla un mazzo con le regole di adesso e le copie che il suo proprietario possiede adesso: un mazzo
     * valido quando è stato salvato può non esserlo più (scambio, carta disattivata).
     *
     * @param deck il mazzo, anche di un altro giocatore (es. quello di chi ha lanciato una sfida)
     * @return il mazzo con le carte in ordine di codice e i problemi
     */
    @Transactional(readOnly = true)
    public DeckCheck checkDeck(Deck deck) {
        List<DeckCards> deckCards = deckCardsRepository.findAllByDeck_Id(deck.getId()).stream()
                .sorted(Comparator.comparing(deckCard -> deckCard.getCard().getCode()))
                .toList();
        Map<Card, Integer> quantities = new LinkedHashMap<>();
        deckCards.forEach(deckCard -> quantities.put(deckCard.getCard(), deckCard.getQuantity()));
        List<String> problems = deckValidator.findProblems(toEntries(quantities),
                collectionService.ownedQuantities(deck.getUser().getId()));
        return new DeckCheck(deck, deckCards, problems);
    }

    // Le carte della richiesta con le copie sommate, dopo il controllo delle regole
    private Map<Card, Integer> validCards(UUID userId, DeckRequest request) {
        Map<UUID, Integer> quantityByCardId = new LinkedHashMap<>();
        for (DeckCardRequest cardRequest : request.cards()) {
            quantityByCardId.merge(cardRequest.cardId(), cardRequest.quantity(), Integer::sum);
        }
        Map<UUID, Card> cardsById = cardRepository.findAllById(quantityByCardId.keySet()).stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));
        if (cardsById.size() < quantityByCardId.size()) {
            throw new InvalidDeckException(List.of(UNKNOWN_CARD_PROBLEM));
        }
        Map<Card, Integer> cards = new LinkedHashMap<>();
        quantityByCardId.forEach((cardId, quantity) -> cards.put(cardsById.get(cardId), quantity));
        List<String> problems = deckValidator.findProblems(toEntries(cards),
                collectionService.ownedQuantities(userId));
        if (!problems.isEmpty()) {
            throw new InvalidDeckException(problems);
        }
        return cards;
    }

    private void saveCards(Deck deck, Map<Card, Integer> cards) {
        cards.forEach((card, quantity) -> deckCardsRepository.save(new DeckCards(deck, card, quantity)));
    }

    private DeckResponse toResponse(Deck deck, User user) {
        List<DeckCards> deckCards = deckCardsRepository.findAllByDeck_Id(deck.getId());
        Map<UUID, CardResponse> cardResponses = toCardResponses(deckCards.stream().map(DeckCards::getCard).toList());
        return toResponse(deck, deckCards, collectionService.ownedQuantities(user.getId()), rankedDeckId(user),
                cardResponses);
    }

    private DeckResponse toResponse(Deck deck, List<DeckCards> deckCards, Map<UUID, Integer> owned,
            UUID rankedDeckId, Map<UUID, CardResponse> cardResponses) {
        List<DeckCards> sortedCards = deckCards.stream()
                .sorted(Comparator.comparing(deckCard -> deckCard.getCard().getCode()))
                .toList();
        Map<Card, Integer> quantities = new LinkedHashMap<>();
        sortedCards.forEach(deckCard -> quantities.put(deckCard.getCard(), deckCard.getQuantity()));
        List<String> problems = deckValidator.findProblems(toEntries(quantities), owned);
        List<DeckCardResponse> cards = sortedCards.stream()
                .map(deckCard -> new DeckCardResponse(cardResponses.get(deckCard.getCard().getId()),
                        deckCard.getQuantity()))
                .toList();
        int totalCards = sortedCards.stream().mapToInt(DeckCards::getQuantity).sum();
        return new DeckResponse(deck.getId(), deck.getName(), totalCards, problems.isEmpty(),
                deck.getId().equals(rankedDeckId), problems, cards);
    }

    // Gli oggetti carta per id, con gli effetti letti in una sola query
    private Map<UUID, CardResponse> toCardResponses(List<Card> cards) {
        List<Card> distinctCards = cards.stream().distinct().toList();
        List<CardResponse> responses = cardMapper.toResponses(distinctCards);
        Map<UUID, CardResponse> byId = new LinkedHashMap<>();
        for (int i = 0; i < distinctCards.size(); i++) {
            byId.put(distinctCards.get(i).getId(), responses.get(i));
        }
        return byId;
    }

    private static List<DeckValidator.Entry> toEntries(Map<Card, Integer> cards) {
        List<DeckValidator.Entry> entries = new ArrayList<>();
        cards.forEach((card, quantity) -> entries.add(new DeckValidator.Entry(card, quantity)));
        return entries;
    }

    private static UUID rankedDeckId(User user) {
        return user.getRankedDeck() != null ? user.getRankedDeck().getId() : null;
    }
}
