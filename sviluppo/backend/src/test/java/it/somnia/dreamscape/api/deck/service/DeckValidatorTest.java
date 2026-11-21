package it.somnia.dreamscape.api.deck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.common.config.GameProperties;
import it.somnia.dreamscape.api.deck.service.DeckValidator.Entry;

// Le regole dei mazzi senza database: carte e rarità sono finte, limiti come in V100 (decisione 017)
class DeckValidatorTest {

    private static final int DECK_SIZE = 20;

    private final Rarity common = rarity("COMMON", "Comune", 1, null, 2);
    private final Rarity rare = rarity("RARE", "Rara", 2, 8, 2);
    private final Rarity legendary = rarity("LEGENDARY", "Leggendaria", 4, 2, 1);

    private final DeckValidator validator = new DeckValidator(
            new GameProperties(500, 50, DECK_SIZE, ZoneId.of("Europe/Rome"),
                    new GameProperties.RankedWinReward(30, 3)));

    // Le copie possedute: per i test, di solito tutte quelle che servono
    private final Map<UUID, Integer> owned = new HashMap<>();

    @Test
    void findProblems_twentyOwnedCardsWithinLimits_returnsNoProblems() {
        // Given: 10 Comuni da 2 copie
        List<Entry> entries = commons(10, 2);

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).isEmpty();
    }

    @Test
    void findProblems_eighteenCards_reportsTheTotal() {
        // Given
        List<Entry> entries = commons(9, 2);

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly("Il mazzo ha 18 carte: ne servono 20.");
    }

    @Test
    void findProblems_threeCopiesOfACommon_reportsTheMaxCopies() {
        // Given: 3 copie della prima Comune, 17 carte nelle altre
        List<Entry> entries = new ArrayList<>(commons(8, 2));
        entries.add(ownedEntry(card("C-09", "Comune Nove", common), 3));
        entries.add(ownedEntry(card("C-10", "Comune Dieci", common), 1));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly("Le copie di Comune Nove sono 3: il massimo è 2.");
    }

    @Test
    void findProblems_legendaryAndItsFullArt_countTogether() {
        // Given: una Leggendaria e la sua full art, 1 copia ciascuna (massimo 1 in tutto)
        Card lord = card("L-01", "Il Sogno Lucido", legendary);
        Card lordFullArt = fullArt("L-01-FA", lord);
        List<Entry> entries = new ArrayList<>(commons(9, 2));
        entries.add(ownedEntry(lord, 1));
        entries.add(ownedEntry(lordFullArt, 1));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly(
                "Le copie di Il Sogno Lucido sono 2: il massimo è 1 (normale e full art contano insieme).");
    }

    @Test
    void findProblems_nineRares_reportsTheRarityLimit() {
        // Given: 5 Rare (2+2+2+2+1 = 9 copie) e 11 Comuni
        List<Entry> entries = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            entries.add(ownedEntry(card("R-0" + i, "Rara " + i, rare), i < 5 ? 2 : 1));
        }
        entries.addAll(commons(5, 2));
        entries.add(ownedEntry(card("C-06", "Comune Sei", common), 1));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly("Le Rare sono 9: il massimo è 8.");
    }

    @Test
    void findProblems_cardNotOwned_reportsIt() {
        // Given
        List<Entry> entries = new ArrayList<>(commons(9, 2));
        entries.add(new Entry(card("C-10", "Lucciola", common), 2));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly("Non possiedi la carta Lucciola.");
    }

    @Test
    void findProblems_fewerCopiesOwned_reportsHowMany() {
        // Given: 2 copie nel mazzo, 1 posseduta (es. l'altra è stata scambiata)
        Card firefly = card("C-10", "Lucciola", common);
        owned.put(firefly.getId(), 1);
        List<Entry> entries = new ArrayList<>(commons(9, 2));
        entries.add(new Entry(firefly, 2));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly("Possiedi 1 copia di Lucciola: nel mazzo ce ne sono 2.");
    }

    @Test
    void findProblems_inactiveCard_reportsItIsNoLongerAvailable() {
        // Given: carta disattivata dall'admin (decisione 026)
        Card lighthouse = card("C-10", "Il Faro nella Nebbia", common);
        when(lighthouse.isActive()).thenReturn(false);
        List<Entry> entries = new ArrayList<>(commons(9, 2));
        entries.add(ownedEntry(lighthouse, 2));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly("La carta Il Faro nella Nebbia non è più disponibile.");
    }

    @Test
    void findProblems_fullArtOfInactiveCard_reportsTheFullArtToo() {
        // Given: disattivare la carta base disattiva anche le sue versioni
        Card lord = card("L-01", "Il Sogno Lucido", legendary);
        when(lord.isActive()).thenReturn(false);
        Card lordFullArt = fullArt("L-01-FA", lord);
        List<Entry> entries = new ArrayList<>(commons(9, 2));
        entries.add(ownedEntry(lordFullArt, 1));
        entries.add(ownedEntry(card("C-10", "Comune Dieci", common), 1));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then
        assertThat(problems).containsExactly("La carta Il Sogno Lucido (full art) non è più disponibile.");
    }

    @Test
    void findProblems_manyErrors_returnsThemAllInAStableOrder() {
        // Given: 3 carte in tutto, una non posseduta con 3 copie
        Card firefly = card("C-01", "Lucciola", common);
        List<Entry> entries = List.of(new Entry(firefly, 3));

        // When
        List<String> problems = validator.findProblems(entries, owned);

        // Then: totale, carte una per una, copie
        assertThat(problems).containsExactly(
                "Il mazzo ha 3 carte: ne servono 20.",
                "Non possiedi la carta Lucciola.",
                "Le copie di Lucciola sono 3: il massimo è 2.");
    }

    // count Comuni diverse da copies copie ciascuna, tutte possedute
    private List<Entry> commons(int count, int copies) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            entries.add(ownedEntry(card(String.format("C-%02d", i), "Comune " + i, common), copies));
        }
        return entries;
    }

    private Entry ownedEntry(Card card, int quantity) {
        owned.put(card.getId(), quantity);
        return new Entry(card, quantity);
    }

    private static Card card(String code, String name, Rarity rarity) {
        Card card = mock(Card.class, withSettings().stubOnly());
        when(card.getId()).thenReturn(UUID.randomUUID());
        when(card.getCode()).thenReturn(code);
        when(card.getName()).thenReturn(name);
        when(card.getRarity()).thenReturn(rarity);
        when(card.isActive()).thenReturn(true);
        return card;
    }

    // Versione senza dati propri: nome e rarità si leggono dalla carta base (decisione 025)
    private static Card fullArt(String code, Card base) {
        Card card = mock(Card.class, withSettings().stubOnly());
        when(card.getId()).thenReturn(UUID.randomUUID());
        when(card.getCode()).thenReturn(code);
        when(card.getBaseCard()).thenReturn(base);
        when(card.getVariantType()).thenReturn(CardVariantType.FULL_ART);
        when(card.isActive()).thenReturn(true);
        return card;
    }

    private static Rarity rarity(String code, String name, int sortOrder, Integer maxPerDeck, int maxCopies) {
        Rarity rarity = mock(Rarity.class, withSettings().stubOnly());
        when(rarity.getId()).thenReturn(UUID.randomUUID());
        when(rarity.getCode()).thenReturn(code);
        when(rarity.getName()).thenReturn(name);
        when(rarity.getSortOrder()).thenReturn(sortOrder);
        when(rarity.getMaxPerDeck()).thenReturn(maxPerDeck);
        when(rarity.getMaxCopies()).thenReturn(maxCopies);
        return rarity;
    }
}
