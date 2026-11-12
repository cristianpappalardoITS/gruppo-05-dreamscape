package it.somnia.dreamscape.api.pack.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackSlot;
import it.somnia.dreamscape.api.pack.model.PackSlotRarity;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.DrawableCardRepository;
import lombok.RequiredArgsConstructor;

/**
 * Estrae le carte di un pacchetto, sempre lato server (schema.md, "Acquisto di un pacchetto").
 *
 * <p>Per ogni slot e per ogni carta dello slot:
 * <ol>
 *     <li>sceglie la rarità con i pesi dello slot;</li>
 *     <li>pesca a caso, con la stessa probabilità per ogni carta, tra le carte normali attive di quella
 *         rarità (e della fazione e dell'espansione, se il pacchetto le indica);</li>
 *     <li>se la carta ha una full art attiva, con probabilità {@code full_art_chance} esce la full art.</li>
 * </ol>
 * Nel set iniziale (pacchetti STARTER) nessuna carta supera le copie massime della sua rarità: così le
 * 20 carte formano sempre un mazzo valido (decisione 014). Gli altri pacchetti non hanno questo limite.
 *
 * <p>Non salva niente: crediti, apertura e collezione li gestisce chi lo chiama, nella stessa transazione.
 */
@Component
@RequiredArgsConstructor
public class PackDrawer {

    private static final double PERCENT = 100.0;

    private final DrawableCardRepository drawableCardRepository;
    private final RandomGenerator random;

    /**
     * Estrae le carte di un pacchetto.
     *
     * @param pack il pacchetto, con i suoi slot
     * @return le carte uscite, nell'ordine degli slot; per una full art c'è la carta della full art
     * @throws IllegalStateException se per una rarità di uno slot non c'è nessuna carta da pescare
     *                               (nel set iniziale: nessuna carta che non abbia già le copie massime)
     */
    public List<Card> draw(Pack pack) {
        boolean limitCopies = pack.getPackType() == PackType.STARTER;
        // Le carte di una rarità si leggono una volta sola per pacchetto
        Map<UUID, List<Card>> candidatesByRarity = new HashMap<>();
        // Copie già uscite di ogni carta normale, per il limite del set iniziale
        Map<UUID, Integer> copiesByCardId = new HashMap<>();
        List<Card> drawn = new ArrayList<>();
        for (PackSlot slot : pack.getSlots()) {
            for (int position = 0; position < slot.getCardCount(); position++) {
                Rarity rarity = pickRarity(slot);
                List<Card> candidates = candidatesByRarity.computeIfAbsent(rarity.getId(),
                        rarityId -> findCandidates(pack, rarityId));
                if (limitCopies) {
                    candidates = candidates.stream()
                            .filter(card -> copiesByCardId.getOrDefault(card.getId(), 0) < rarity.getMaxCopies())
                            .toList();
                }
                if (candidates.isEmpty()) {
                    throw new IllegalStateException(
                            "Nessuna carta " + rarity.getCode() + " da pescare per il pacchetto " + pack.getName());
                }
                Card card = candidates.get(random.nextInt(candidates.size()));
                copiesByCardId.merge(card.getId(), 1, Integer::sum);
                drawn.add(maybeFullArt(pack, card));
            }
        }
        return List.copyOf(drawn);
    }

    // Probabilità di una rarità = suo peso / somma dei pesi dello slot. Ordine fisso (dalla più comune)
    // perché con lo stesso numero casuale esca sempre la stessa rarità.
    private Rarity pickRarity(PackSlot slot) {
        List<PackSlotRarity> rarities = slot.getRarities().stream()
                .sorted(Comparator.comparingInt(slotRarity -> slotRarity.getRarity().getSortOrder()))
                .toList();
        int totalWeight = rarities.stream().mapToInt(PackSlotRarity::getWeight).sum();
        int roll = random.nextInt(totalWeight);
        for (PackSlotRarity slotRarity : rarities) {
            roll -= slotRarity.getWeight();
            if (roll < 0) {
                return slotRarity.getRarity();
            }
        }
        throw new IllegalStateException("Pesi dello slot non validi: totale " + totalWeight);
    }

    private List<Card> findCandidates(Pack pack, UUID rarityId) {
        UUID factionId = pack.getFaction() != null ? pack.getFaction().getId() : null;
        UUID cardSetId = pack.getCardSet() != null ? pack.getCardSet().getId() : null;
        return drawableCardRepository.findDrawable(rarityId, factionId, cardSetId);
    }

    // Il tiro per la full art si fa solo se la carta ne ha una attiva
    private Card maybeFullArt(Pack pack, Card card) {
        double chance = pack.getFullArtChance().doubleValue();
        if (chance <= 0) {
            return card;
        }
        return drawableCardRepository.findActiveVariant(card, CardVariantType.FULL_ART)
                .filter(fullArt -> random.nextDouble() * PERCENT < chance)
                .orElse(card);
    }
}
