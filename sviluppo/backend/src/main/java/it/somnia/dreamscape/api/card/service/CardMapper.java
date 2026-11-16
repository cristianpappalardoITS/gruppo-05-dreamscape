package it.somnia.dreamscape.api.card.service;

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
import lombok.RequiredArgsConstructor;

/**
 * Trasforma le carte del database negli oggetti "Carta" della guida per il frontend, con i loro effetti.
 */
@Component
@RequiredArgsConstructor
public class CardMapper {

    private final CardEffectRepository cardEffectRepository;

    /**
     * Restituisce le carte come le riceve il frontend, nello stesso ordine.
     *
     * <p>Gli effetti stanno sulle carte normali: per una full art si usa quello della sua carta base.
     * Gli effetti si leggono con una sola query per tutte le carte.
     *
     * @param cards le carte da trasformare
     * @return una {@link CardResponse} per ogni carta
     */
    public List<CardResponse> toResponses(List<Card> cards) {
        List<Card> baseCards = cards.stream().map(CardMapper::baseOf).distinct().toList();
        Map<UUID, CardEffect> effectsByCardId = cardEffectRepository.findByCardIn(baseCards).stream()
                .collect(Collectors.toMap(effect -> effect.getCard().getId(), Function.identity()));
        return cards.stream()
                .map(card -> CardResponse.from(card, effectsByCardId.get(baseOf(card).getId())))
                .toList();
    }

    /**
     * Restituisce una carta come la riceve il frontend.
     *
     * @param card la carta, normale o full art
     * @return la carta con l'effetto della sua carta base
     */
    public CardResponse toResponse(Card card) {
        CardEffect effect = cardEffectRepository.findByCard(baseOf(card)).orElse(null);
        return CardResponse.from(card, effect);
    }

    private static Card baseOf(Card card) {
        return card.getBaseCard() != null ? card.getBaseCard() : card;
    }
}
