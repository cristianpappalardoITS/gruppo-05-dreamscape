package it.somnia.dreamscape.api.trade.service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.trade.repository.OfferedCopiesView;
import it.somnia.dreamscape.api.trade.repository.TradeItemRepository;
import lombok.RequiredArgsConstructor;

/**
 * Le copie bloccate negli scambi (decisione 024): quelle che un giocatore offre negli scambi in attesa che ha
 * proposto. Non le può offrire in un altro scambio; restano usabili nei mazzi. Nessuna colonna in più: si
 * contano da {@code trade_items}. Lo usano gli scambi e la collezione ({@code availableForTrade}).
 */
@Component
@RequiredArgsConstructor
public class LockedCopies {

    private final TradeItemRepository tradeItemRepository;

    /**
     * Le copie già offerte negli scambi in attesa, per id della carta (la full art ha il suo id).
     *
     * @param userId il giocatore
     * @return copie bloccate per carta; le carte senza copie bloccate non ci sono
     */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> of(UUID userId) {
        return tradeItemRepository.sumOfferedInPendingTrades(userId).stream()
                .collect(Collectors.toMap(OfferedCopiesView::getCardId, copies -> (int) copies.getQuantity()));
    }
}
