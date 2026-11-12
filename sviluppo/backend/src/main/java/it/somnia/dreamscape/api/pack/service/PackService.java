package it.somnia.dreamscape.api.pack.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;
import it.somnia.dreamscape.api.card.repository.CardEffectRepository;
import it.somnia.dreamscape.api.deck.model.UserCards;
import it.somnia.dreamscape.api.deck.repository.UserCardsRepository;
import it.somnia.dreamscape.api.pack.dto.request.OpenPackRequest;
import it.somnia.dreamscape.api.pack.dto.response.OpenPackResponse;
import it.somnia.dreamscape.api.pack.dto.response.OpenedCardResponse;
import it.somnia.dreamscape.api.pack.dto.response.PackOpeningResponse;
import it.somnia.dreamscape.api.pack.dto.response.PackResponse;
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackCover;
import it.somnia.dreamscape.api.pack.model.PackOpening;
import it.somnia.dreamscape.api.pack.model.PackOpeningCard;
import it.somnia.dreamscape.api.pack.model.PackOpeningSource;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import it.somnia.dreamscape.api.pack.service.PackExceptions.InvalidCoverException;
import it.somnia.dreamscape.api.pack.service.PackExceptions.PackNotFoundException;
import it.somnia.dreamscape.api.pack.service.PackExceptions.PackNotOnSaleException;
import it.somnia.dreamscape.api.pack.service.PackExceptions.PackOpeningNotFoundException;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.service.CreditService;
import it.somnia.dreamscape.api.user.service.UserExceptions.NotEnoughCreditsException;
import it.somnia.dreamscape.api.user.service.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Il negozio dei pacchetti e le aperture: quali pacchetti sono in vendita, l'acquisto e le carte uscite.
 */
@Service
@RequiredArgsConstructor
public class PackService {

    private final PackRepository packRepository;
    private final PackOpeningRepository packOpeningRepository;
    private final CardEffectRepository cardEffectRepository;
    private final UserCardsRepository userCardsRepository;
    private final PackDrawer packDrawer;
    private final UserService userService;
    private final CreditService creditService;
    private final Clock clock;

    /**
     * Restituisce i pacchetti in vendita in questo momento ({@code GET /packs}), dal più economico.
     *
     * <p>Un pacchetto è in vendita se è di tipo SHOP, attivo e dentro il suo periodo di vendita.
     * Per ogni pacchetto ci sono le copertine attive e gli slot con le probabilità delle rarità.
     *
     * @return i pacchetti in vendita, lista vuota se non ce n'è nessuno
     */
    @Transactional(readOnly = true)
    public List<PackResponse> listPacksOnSale() {
        return packRepository.findOnSale(PackType.SHOP, clock.instant()).stream()
                .map(PackResponse::from)
                .toList();
    }

    /**
     * Compra un pacchetto e lo apre subito ({@code POST /packs/{packId}/open}).
     *
     * <p>Tutto in una transazione, o niente: blocca la riga dell'utente, controlla pacchetto, copertina
     * e saldo, estrae le carte, salva l'apertura, scala i crediti e aggiunge le carte alla collezione.
     * Con la riga bloccata, due acquisti insieme dello stesso utente si mettono in fila: il secondo vede
     * il saldo già scalato.
     *
     * @param userId  l'utente collegato
     * @param packId  il pacchetto da comprare
     * @param request la copertina scelta; può essere {@code null}
     * @return l'id dell'apertura, il saldo dopo l'acquisto e le carte uscite
     * @throws PackNotFoundException     se il pacchetto non esiste o non si compra
     * @throws PackNotOnSaleException    se il pacchetto è spento o fuori dal periodo di vendita
     * @throws InvalidCoverException     se la copertina non è una copertina attiva del pacchetto
     * @throws NotEnoughCreditsException se il saldo non basta
     */
    @Transactional
    public OpenPackResponse openPack(UUID userId, UUID packId, OpenPackRequest request) {
        User user = userService.getActiveUserForUpdate(userId);
        Pack pack = packRepository.findById(packId)
                .filter(candidate -> candidate.getPackType() == PackType.SHOP)
                .orElseThrow(PackNotFoundException::new);
        if (!pack.isOnSale(clock.instant())) {
            throw new PackNotOnSaleException();
        }
        PackCover cover = findCover(pack, request != null ? request.coverId() : null);
        // Prima di estrarre: senza crediti non si fa lavoro inutile
        if (user.getCredits() < pack.getPriceCredits()) {
            throw new NotEnoughCreditsException();
        }

        List<Card> cards = packDrawer.draw(pack);
        PackOpening opening = new PackOpening(user, pack, cover, PackOpeningSource.PURCHASE, pack.getPriceCredits());
        cards.forEach(opening::addCard);
        packOpeningRepository.save(opening);
        int creditsAfter = creditService.apply(user, CreditTransactionType.PACK_PURCHASE, -pack.getPriceCredits(),
                opening.getId(), null).getBalanceAfter();
        addToCollection(user, cards);
        return new OpenPackResponse(opening.getId(), creditsAfter, toOpenedCards(cards));
    }

    /**
     * Restituisce un'apertura dell'utente con le carte uscite ({@code GET /pack-openings/{openingId}}).
     *
     * @param userId    l'utente collegato
     * @param openingId l'apertura da leggere
     * @return l'apertura, con le carte nell'ordine in cui sono uscite
     * @throws PackOpeningNotFoundException se l'apertura non esiste o è di un altro utente
     */
    @Transactional(readOnly = true)
    public PackOpeningResponse getOpening(UUID userId, UUID openingId) {
        PackOpening opening = packOpeningRepository.findByIdAndUserId(openingId, userId)
                .orElseThrow(PackOpeningNotFoundException::new);
        List<Card> cards = opening.getCards().stream().map(PackOpeningCard::getCard).toList();
        return new PackOpeningResponse(opening.getId(), opening.getPack().getName(), opening.getOpenedAt(),
                toOpenedCards(cards));
    }

    // La copertina è facoltativa; se c'è, deve essere una copertina attiva di questo pacchetto
    private static PackCover findCover(Pack pack, UUID coverId) {
        if (coverId == null) {
            return null;
        }
        return pack.getCovers().stream()
                .filter(cover -> cover.getId().equals(coverId) && cover.isActive())
                .findFirst()
                .orElseThrow(InvalidCoverException::new);
    }

    // Una riga per carta posseduta: se c'è già si aumentano le copie, altrimenti si crea.
    // La full art è una carta a parte, quindi ha la sua riga.
    private void addToCollection(User user, List<Card> cards) {
        Map<UUID, UserCards> owned = new HashMap<>();
        userCardsRepository.findUserCardsByUser_Id(user.getId())
                .forEach(userCard -> owned.put(userCard.getCard().getId(), userCard));
        for (Card card : cards) {
            UserCards userCard = owned.get(card.getId());
            if (userCard != null) {
                userCard.setQuantity(userCard.getQuantity() + 1);
            } else {
                owned.put(card.getId(), userCardsRepository.save(new UserCards(user, card, 1)));
            }
        }
    }

    // Gli effetti stanno sulle carte normali: per una full art si usa quello della sua carta base
    private List<OpenedCardResponse> toOpenedCards(List<Card> cards) {
        List<Card> baseCards = cards.stream().map(PackService::baseOf).distinct().toList();
        Map<UUID, CardEffect> effectsByCardId = cardEffectRepository.findByCardIn(baseCards).stream()
                .collect(Collectors.toMap(effect -> effect.getCard().getId(), Function.identity()));
        return cards.stream()
                .map(card -> new OpenedCardResponse(
                        CardResponse.from(card, effectsByCardId.get(baseOf(card).getId()))))
                .toList();
    }

    private static Card baseOf(Card card) {
        return card.getBaseCard() != null ? card.getBaseCard() : card;
    }
}
