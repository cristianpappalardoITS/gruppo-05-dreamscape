package it.somnia.dreamscape.api.trade.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.somnia.dreamscape.api.trade.model.TradeItem;

public interface TradeItemRepository extends JpaRepository<TradeItem, UUID> {

    // Le copie bloccate (decisione 024): quelle che il giocatore offre negli scambi PENDING che ha proposto.
    // La query di docs/database/schema.md (sezione Scambi), per tutte le carte insieme
    @Query("""
            SELECT ti.card.id AS cardId, SUM(ti.quantity) AS quantity
            FROM TradeItem ti
            WHERE ti.trade.proposer.id = :userId
              AND ti.trade.status = it.somnia.dreamscape.api.trade.model.TradeStatus.PENDING
              AND ti.side = it.somnia.dreamscape.api.trade.model.TradeSide.OFFERED
            GROUP BY ti.card.id
            """)
    List<OfferedCopiesView> sumOfferedInPendingTrades(UUID userId);

    // Le carte di più scambi insieme, con la carta già caricata (liste)
    @Query("SELECT ti FROM TradeItem ti JOIN FETCH ti.card WHERE ti.trade.id IN :tradeIds")
    List<TradeItem> findWithCardByTradeIds(List<UUID> tradeIds);
}
