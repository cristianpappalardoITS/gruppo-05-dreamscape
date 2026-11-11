package it.somnia.dreamscape.api.pack.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;

// Le carte che possono uscire da un pacchetto. Sta in pack/ perché queste regole sono dell'estrazione
// (schema.md, "Acquisto di un pacchetto"), non del catalogo.
public interface DrawableCardRepository extends Repository<Card, UUID> {

    // Carte normali attive di una rarità; factionId e cardSetId null = nessun filtro.
    // Senza espansione indicata si pesca solo dalle espansioni attive. Ordine fisso per avere estrazioni
    // ripetibili nei test.
    @Query("""
            SELECT c FROM Card c
            WHERE c.baseCard IS NULL AND c.active = TRUE AND c.rarity.id = :rarityId
              AND (:factionId IS NULL OR c.faction.id = :factionId)
              AND ((:cardSetId IS NULL AND c.cardSet.active = TRUE) OR c.cardSet.id = :cardSetId)
            ORDER BY c.code ASC""")
    List<Card> findDrawable(@Param("rarityId") UUID rarityId, @Param("factionId") UUID factionId,
            @Param("cardSetId") UUID cardSetId);

    // La versione attiva di una carta, es. la full art che esce al posto della carta normale
    @Query("SELECT c FROM Card c WHERE c.baseCard = :baseCard AND c.variantType = :variantType AND c.active = TRUE")
    Optional<Card> findActiveVariant(@Param("baseCard") Card baseCard,
            @Param("variantType") CardVariantType variantType);
}
