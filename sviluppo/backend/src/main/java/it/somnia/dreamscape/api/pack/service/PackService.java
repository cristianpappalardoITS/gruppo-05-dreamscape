package it.somnia.dreamscape.api.pack.service;

import java.time.Clock;
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
import it.somnia.dreamscape.api.pack.dto.response.OpenedCardResponse;
import it.somnia.dreamscape.api.pack.dto.response.PackOpeningResponse;
import it.somnia.dreamscape.api.pack.dto.response.PackResponse;
import it.somnia.dreamscape.api.pack.model.PackOpening;
import it.somnia.dreamscape.api.pack.model.PackOpeningCard;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.pack.repository.PackRepository;
import it.somnia.dreamscape.api.pack.service.PackExceptions.PackOpeningNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Il negozio dei pacchetti e le aperture: quali pacchetti sono in vendita e quali carte sono uscite.
 */
@Service
@RequiredArgsConstructor
public class PackService {

    private final PackRepository packRepository;
    private final PackOpeningRepository packOpeningRepository;
    private final CardEffectRepository cardEffectRepository;
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
