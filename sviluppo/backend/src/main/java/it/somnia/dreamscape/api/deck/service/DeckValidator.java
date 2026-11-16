package it.somnia.dreamscape.api.deck.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.common.config.GameProperties;
import lombok.RequiredArgsConstructor;

/**
 * Controlla le regole di un mazzo e restituisce i problemi da mostrare al giocatore (decisioni 017, 025 e 026).
 *
 * <p>Si usa al salvataggio, dove un solo problema blocca il mazzo, e alla lettura, dove i problemi dicono
 * perché un mazzo già salvato non è più valido (scambio, carta disattivata). Le regole:
 * <ul>
 *   <li>il numero di carte della configurazione ({@code dreamscape.game.deck-size}, oggi 20);</li>
 *   <li>tutte le carte attive, anche la carta base di una full art;</li>
 *   <li>abbastanza copie possedute di ogni carta (la full art si conta da sola);</li>
 *   <li>copie massime della rarità, contando insieme carta normale e full art;</li>
 *   <li>carte massime per rarità nel mazzo ({@code rarities.max_per_deck}, null = nessun limite).</li>
 * </ul>
 * I limiti di rarità si leggono dal database, così valgono anche se l'admin li cambia.
 */
@Component
@RequiredArgsConstructor
public class DeckValidator {

    // Il nome al plurale per i messaggi sui limiti, es. "Le Rare sono 9"
    private static final Map<String, String> RARITY_PLURALS = Map.of(
            "COMMON", "Comuni",
            "RARE", "Rare",
            "EPIC", "Epiche",
            "LEGENDARY", "Leggendarie");

    private final GameProperties gameProperties;

    /**
     * Una carta del mazzo con le sue copie.
     *
     * @param card     la carta, normale o full art
     * @param quantity le copie nel mazzo, almeno 1
     */
    public record Entry(Card card, int quantity) {
    }

    /**
     * Restituisce i problemi del mazzo, in un ordine stabile: totale, carte una per una, copie, rarità.
     *
     * @param entries        le carte del mazzo, una voce per carta
     * @param ownedByCardId  le copie possedute dal giocatore, per id della carta
     * @return i problemi in italiano, lista vuota se il mazzo è valido
     */
    public List<String> findProblems(List<Entry> entries, Map<UUID, Integer> ownedByCardId) {
        List<String> problems = new ArrayList<>();
        List<Entry> sortedEntries = entries.stream()
                .sorted(Comparator.comparing(entry -> entry.card().getCode()))
                .toList();

        int totalCards = sortedEntries.stream().mapToInt(Entry::quantity).sum();
        if (totalCards != gameProperties.deckSize()) {
            problems.add("Il mazzo ha " + countOf(totalCards, "carta", "carte") + ": ne servono "
                    + gameProperties.deckSize() + ".");
        }

        for (Entry entry : sortedEntries) {
            Card card = entry.card();
            if (!card.isActive() || !baseOf(card).isActive()) {
                problems.add("La carta " + label(card) + " non è più disponibile.");
            }
            int owned = ownedByCardId.getOrDefault(card.getId(), 0);
            if (owned == 0) {
                problems.add("Non possiedi la carta " + label(card) + ".");
            } else if (owned < entry.quantity()) {
                problems.add("Possiedi " + countOf(owned, "copia", "copie") + " di " + label(card)
                        + ": nel mazzo ce ne sono " + entry.quantity() + ".");
            }
        }

        problems.addAll(copiesProblems(sortedEntries));
        problems.addAll(rarityProblems(sortedEntries));
        return problems;
    }

    // Copie massime per carta: le versioni della stessa carta contano insieme (decisione 025)
    // Le mappe usano gli id: con Hibernate la stessa carta base può arrivare come entità o come proxy
    private static List<String> copiesProblems(List<Entry> sortedEntries) {
        Map<UUID, Card> baseById = new LinkedHashMap<>();
        Map<UUID, Integer> copiesByBaseId = new LinkedHashMap<>();
        Map<UUID, Boolean> hasVersionsByBaseId = new LinkedHashMap<>();
        for (Entry entry : sortedEntries) {
            Card base = baseOf(entry.card());
            baseById.putIfAbsent(base.getId(), base);
            copiesByBaseId.merge(base.getId(), entry.quantity(), Integer::sum);
            hasVersionsByBaseId.merge(base.getId(), entry.card().getBaseCard() != null, Boolean::logicalOr);
        }
        List<String> problems = new ArrayList<>();
        copiesByBaseId.forEach((baseId, copies) -> {
            Card base = baseById.get(baseId);
            int maxCopies = base.getRarity().getMaxCopies();
            if (copies > maxCopies) {
                String together = hasVersionsByBaseId.get(baseId) ? " (normale e full art contano insieme)" : "";
                problems.add("Le copie di " + base.getName() + " sono " + copies + ": il massimo è " + maxCopies
                        + together + ".");
            }
        });
        return problems;
    }

    // Carte massime per rarità, dalla Comune alla Leggendaria
    private static List<String> rarityProblems(List<Entry> sortedEntries) {
        Map<UUID, Rarity> rarityById = new LinkedHashMap<>();
        Map<UUID, Integer> cardsByRarityId = new LinkedHashMap<>();
        sortedEntries.stream()
                .sorted(Comparator.comparingInt(entry -> baseOf(entry.card()).getRarity().getSortOrder()))
                .forEach(entry -> {
                    Rarity rarity = baseOf(entry.card()).getRarity();
                    rarityById.putIfAbsent(rarity.getId(), rarity);
                    cardsByRarityId.merge(rarity.getId(), entry.quantity(), Integer::sum);
                });
        List<String> problems = new ArrayList<>();
        cardsByRarityId.forEach((rarityId, count) -> {
            Rarity rarity = rarityById.get(rarityId);
            Integer maxPerDeck = rarity.getMaxPerDeck();
            if (maxPerDeck != null && count > maxPerDeck) {
                String plural = RARITY_PLURALS.getOrDefault(rarity.getCode(), "Carte " + rarity.getName());
                problems.add("Le " + plural + " sono " + count + ": il massimo è " + maxPerDeck + ".");
            }
        });
        return problems;
    }

    // Il nome della carta per il giocatore; la full art ha lo stesso nome della carta base
    private static String label(Card card) {
        String name = baseOf(card).getName();
        return card.getVariantType() == CardVariantType.FULL_ART ? name + " (full art)" : name;
    }

    private static Card baseOf(Card card) {
        return card.getBaseCard() != null ? card.getBaseCard() : card;
    }

    private static String countOf(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }
}
