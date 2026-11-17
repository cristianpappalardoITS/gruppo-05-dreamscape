package it.somnia.dreamscape.api.deck.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.service.CardMapper;
import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.deck.dto.response.CollectionCardResponse;
import it.somnia.dreamscape.api.deck.model.UserCards;
import it.somnia.dreamscape.api.deck.repository.UserCardsRepository;
import it.somnia.dreamscape.api.deck.repository.UserCardsSpecifications;
import it.somnia.dreamscape.api.trade.service.LockedCopies;
import lombok.RequiredArgsConstructor;

/**
 * La collezione dell'utente collegato: le carte possedute con le loro copie.
 */
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final UserCardsRepository userCardsRepository;
    private final CardMapper cardMapper;
    private final LockedCopies lockedCopies;

    /**
     * Restituisce una pagina della collezione ({@code GET /me/cards}), in ordine di codice della carta.
     *
     * <p>La full art è una voce a parte, subito dopo la sua carta normale ({@code BASE-004}, {@code BASE-004-FA}).
     * Ci sono anche le carte disattivate: chi le possiede le tiene (decisione 026). Fazione, rarità e nome di
     * una full art sono quelli della sua carta base. I filtri vuoti non filtrano.
     *
     * @param userId      l'utente collegato
     * @param factionCode codice della fazione, es. {@code DREAMS}; può essere {@code null}
     * @param rarityCode  codice della rarità, es. {@code RARE}; può essere {@code null}
     * @param search      parte del nome, senza distinguere maiuscole e minuscole; può essere {@code null}
     * @param fullArt     {@code true} solo le full art, {@code false} solo le carte normali, {@code null} tutte
     * @param page        numero della pagina, da 0
     * @param size        voci per pagina
     * @return la pagina della collezione con i totali
     */
    @Transactional(readOnly = true)
    public PageResponse<CollectionCardResponse> listCollection(UUID userId, String factionCode, String rarityCode,
            String search, Boolean fullArt, int page, int size) {
        List<Specification<UserCards>> filters = new ArrayList<>();
        filters.add(UserCardsSpecifications.ownedBy(userId));
        if (hasText(factionCode)) {
            filters.add(UserCardsSpecifications.hasFaction(factionCode.strip()));
        }
        if (hasText(rarityCode)) {
            filters.add(UserCardsSpecifications.hasRarity(rarityCode.strip()));
        }
        if (hasText(search)) {
            filters.add(UserCardsSpecifications.nameContains(search.strip()));
        }
        if (fullArt != null) {
            filters.add(UserCardsSpecifications.isFullArt(fullArt));
        }
        Page<UserCards> userCards = userCardsRepository.findAll(Specification.allOf(filters),
                PageRequest.of(page, size, Sort.by("card.code")));
        List<CardResponse> cards = cardMapper.toResponses(
                userCards.getContent().stream().map(UserCards::getCard).toList());
        Map<UUID, Integer> locked = lockedCopies.of(userId);
        List<CollectionCardResponse> items = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            UserCards userCard = userCards.getContent().get(i);
            int quantity = userCard.getQuantity();
            int availableForTrade = Math.max(0, quantity - locked.getOrDefault(userCard.getCard().getId(), 0));
            items.add(new CollectionCardResponse(cards.get(i), quantity, availableForTrade));
        }
        return new PageResponse<>(items, userCards.getNumber(), userCards.getSize(), userCards.getTotalElements(),
                userCards.getTotalPages());
    }

    /**
     * Restituisce le copie possedute dall'utente, per id della carta (la full art ha il suo id).
     *
     * @param userId l'utente
     * @return copie per id della carta; le carte non possedute non ci sono
     */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> ownedQuantities(UUID userId) {
        return userCardsRepository.findUserCardsByUser_Id(userId).stream()
                .collect(Collectors.toMap(userCard -> userCard.getCard().getId(), UserCards::getQuantity));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
