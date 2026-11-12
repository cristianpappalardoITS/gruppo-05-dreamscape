package it.somnia.dreamscape.api.pack.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;
import it.somnia.dreamscape.api.card.repository.CardEffectRepository;
import it.somnia.dreamscape.api.pack.dto.response.OpenedCardResponse;
import lombok.RequiredArgsConstructor;

/**
 * Trasforma le carte uscite da un pacchetto negli oggetti per il frontend, con i loro effetti.
 */
@Component
@RequiredArgsConstructor
public class OpenedCardMapper {

    private final CardEffectRepository cardEffectRepository;

    /**
     * Restituisce le carte come le riceve il frontend, nello stesso ordine.
     *
     * <p>Gli effetti stanno sulle carte normali: per una full art si usa quello della sua carta base.
     * Gli effetti si leggono con una sola query per tutte le carte.
     *
     * @param cards le carte uscite
     * @return una voce {@code { "card": ... }} per ogni carta
     */
    public List<OpenedCardResponse> toResponses(List<Card> cards) {
        List<Card> baseCards = cards.stream().map(OpenedCardMapper::baseOf).distinct().toList();
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
