package it.somnia.dreamscape.api.trade.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.model.DeckCards;
import it.somnia.dreamscape.api.deck.repository.DeckCardsRepository;
import it.somnia.dreamscape.api.deck.repository.DeckRepository;
import it.somnia.dreamscape.api.deck.service.CollectionService;
import it.somnia.dreamscape.api.deck.service.DeckValidator;
import it.somnia.dreamscape.api.trade.dto.response.DeckWarningOutcome;
import it.somnia.dreamscape.api.trade.dto.response.DeckWarningResponse;
import it.somnia.dreamscape.api.trade.dto.response.LastCopyWarningResponse;
import it.somnia.dreamscape.api.user.model.User;
import lombok.RequiredArgsConstructor;

/**
 * Cosa succede alla collezione e ai mazzi di un giocatore con uno scambio, prima che avvenga.
 *
 * <p>Serve agli avvisi (anteprima della proposta e prima di accettare) e all'accettazione, che usa lo stesso
 * calcolo: così l'avviso dice esattamente quello che poi succede.
 * <ul>
 *   <li><b>Ultima copia:</b> una carta che esce e dopo lo scambio non c'è più nella collezione (la full art si
 *       conta da sola).</li>
 *   <li><b>Mazzo non valido</b> ({@code INVALID}): un mazzo valido usa più copie di una carta che esce di quelle
 *       che restano. Un mazzo già non valido non si segnala.</li>
 *   <li><b>Versione normale</b> ({@code NORMAL_VERSION}): esce una full art usata in un mazzo, ma restano
 *       abbastanza copie libere della carta normale (non già usate in quel mazzo): all'accettazione il mazzo passa
 *       alla versione normale e resta valido (schema.md, sezione Scambi).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TradeImpact {

    private final DeckRepository deckRepository;
    private final DeckCardsRepository deckCardsRepository;
    private final CollectionService collectionService;
    private final DeckValidator deckValidator;

    /**
     * Un passaggio alla versione normale da fare all'accettazione: {@code copies} copie della full art nel mazzo
     * diventano copie della carta normale.
     */
    public record FullArtSwap(Deck deck, DeckCards fullArtEntry, Card normalCard, int copies) {
    }

    /**
     * Il risultato del calcolo per un giocatore.
     */
    public record Impact(List<DeckWarningResponse> deckWarnings, List<LastCopyWarningResponse> lastCopyWarnings,
            List<FullArtSwap> swaps) {
    }

    /**
     * Calcola gli effetti dello scambio sul giocatore.
     *
     * @param player   il giocatore
     * @param given    le copie che il giocatore dà, per id della carta
     * @param received le copie che il giocatore riceve, per id della carta
     * @param cards    le carte dello scambio, per id
     * @return avvisi e passaggi alla versione normale
     */
    public Impact of(User player, Map<UUID, Integer> given, Map<UUID, Integer> received, Map<UUID, Card> cards) {
        Map<UUID, Integer> owned = collectionService.ownedQuantities(player.getId());
        Map<UUID, Integer> ownedAfter = new HashMap<>(owned);
        given.forEach((cardId, copies) -> ownedAfter.merge(cardId, -copies, Integer::sum));
        received.forEach((cardId, copies) -> ownedAfter.merge(cardId, copies, Integer::sum));
        List<Card> givenCards = given.keySet().stream()
                .map(cards::get)
                .sorted(Comparator.comparing(Card::getCode))
                .toList();

        List<LastCopyWarningResponse> lastCopies = givenCards.stream()
                .filter(card -> ownedAfter.getOrDefault(card.getId(), 0) <= 0)
                .map(card -> new LastCopyWarningResponse(card.getId(), TradeService.label(card)))
                .toList();

        List<DeckWarningResponse> deckWarnings = new ArrayList<>();
        List<FullArtSwap> swaps = new ArrayList<>();
        List<Deck> decks = deckRepository.findAllByUser_IdOrderByNameAsc(player.getId());
        if (decks.isEmpty()) {
            return new Impact(List.of(), lastCopies, List.of());
        }
        Map<UUID, List<DeckCards>> entriesByDeck = deckCardsRepository
                .findAllByDeck_IdIn(decks.stream().map(Deck::getId).toList()).stream()
                .collect(Collectors.groupingBy(entry -> entry.getDeck().getId()));
        UUID rankedDeckId = player.getRankedDeck() != null ? player.getRankedDeck().getId() : null;

        for (Deck deck : decks) {
            List<DeckCards> entries = entriesByDeck.getOrDefault(deck.getId(), List.of());
            Map<UUID, DeckCards> entryByCardId = new LinkedHashMap<>();
            entries.forEach(entry -> entryByCardId.put(entry.getCard().getId(), entry));
            boolean validBefore = deckValidator.findProblems(entries.stream()
                    .map(entry -> new DeckValidator.Entry(entry.getCard(), entry.getQuantity()))
                    .toList(), owned).isEmpty();
            boolean isRanked = deck.getId().equals(rankedDeckId);
            // Copie della carta normale già prese da un passaggio in questo mazzo
            Map<UUID, Integer> normalTaken = new HashMap<>();

            for (Card card : givenCards) {
                DeckCards entry = entryByCardId.get(card.getId());
                if (entry == null) {
                    continue;
                }
                int missing = entry.getQuantity() - ownedAfter.getOrDefault(card.getId(), 0);
                if (missing <= 0) {
                    continue;
                }
                String cardName = baseOf(card).getName();
                if (card.getVariantType() == CardVariantType.FULL_ART) {
                    Card normal = baseOf(card);
                    DeckCards normalEntry = entryByCardId.get(normal.getId());
                    int usedInDeck = (normalEntry != null ? normalEntry.getQuantity() : 0)
                            + normalTaken.getOrDefault(normal.getId(), 0);
                    int free = ownedAfter.getOrDefault(normal.getId(), 0) - usedInDeck;
                    if (free >= missing) {
                        normalTaken.merge(normal.getId(), missing, Integer::sum);
                        swaps.add(new FullArtSwap(deck, entry, normal, missing));
                        deckWarnings.add(new DeckWarningResponse(deck.getId(), deck.getName(), isRanked, cardName,
                                DeckWarningOutcome.NORMAL_VERSION));
                        continue;
                    }
                }
                if (validBefore) {
                    deckWarnings.add(new DeckWarningResponse(deck.getId(), deck.getName(), isRanked, cardName,
                            DeckWarningOutcome.INVALID));
                }
            }
        }
        return new Impact(deckWarnings, lastCopies, swaps);
    }

    private static Card baseOf(Card card) {
        return card.getBaseCard() != null ? card.getBaseCard() : card;
    }
}
