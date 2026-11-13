package it.somnia.dreamscape.api.pack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.Faction;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackSlot;
import it.somnia.dreamscape.api.pack.model.PackSlotRarity;
import it.somnia.dreamscape.api.pack.model.PackType;
import it.somnia.dreamscape.api.pack.repository.DrawableCardRepository;

// L'estrazione senza database: pacchetti, slot e carte sono finti, il generatore casuale è controllato
class PackDrawerTest {

    private final Rarity common = rarity("COMMON", 1);
    private final Rarity rare = rarity("RARE", 2);
    private final Rarity epic = rarity("EPIC", 3);
    private final Rarity legendary = rarity("LEGENDARY", 4);

    private final Card commonCard = card("COMMON-1");
    private final Card rareCard = card("RARE-1");
    private final Card epicCard = card("EPIC-1");
    private final Card legendaryCard = card("LEGENDARY-1");
    private final Card legendaryFullArt = card("LEGENDARY-1-FA");

    private final DrawableCardRepository repository = mock(DrawableCardRepository.class);

    PackDrawerTest() {
        when(repository.findDrawable(eq(common.getId()), any(), any())).thenReturn(List.of(commonCard));
        when(repository.findDrawable(eq(rare.getId()), any(), any())).thenReturn(List.of(rareCard));
        when(repository.findDrawable(eq(epic.getId()), any(), any())).thenReturn(List.of(epicCard));
        when(repository.findDrawable(eq(legendary.getId()), any(), any())).thenReturn(List.of(legendaryCard));
        when(repository.findActiveVariant(any(), eq(CardVariantType.FULL_ART))).thenReturn(Optional.empty());
        when(repository.findActiveVariant(legendaryCard, CardVariantType.FULL_ART))
                .thenReturn(Optional.of(legendaryFullArt));
    }

    @Test
    void draw_dreamPackShape_returnsOneCardPerSlotPosition() {
        // Given: 3 Comuni, 1 Rara, 1 slot speciale
        Pack pack = pack("20", slot(3, Map.of(common, 100)), slot(1, Map.of(rare, 100)), specialSlot());
        PackDrawer drawer = new PackDrawer(repository, new Random(42));

        // When
        List<Card> cards = drawer.draw(pack);

        // Then
        assertEquals(5, cards.size());
        assertEquals(List.of(commonCard, commonCard, commonCard, rareCard), cards.subList(0, 4));
    }

    // Pesi 70 / 25 / 5 dalla rarità più comune: 0-69 Rara, 70-94 Epica, 95-99 Leggendaria
    @ParameterizedTest
    @CsvSource({ "0, RARE", "69, RARE", "70, EPIC", "94, EPIC", "95, LEGENDARY", "99, LEGENDARY" })
    void draw_rollInsideWeight_picksThatRarity(int roll, String expectedCode) {
        // Given
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextInt(100)).thenReturn(roll);
        when(random.nextDouble()).thenReturn(0.99);
        PackDrawer drawer = new PackDrawer(repository, random);

        // When
        Card drawn = drawer.draw(pack("20", specialSlot())).getFirst();

        // Then
        Map<String, Card> byCode = Map.of("RARE", rareCard, "EPIC", epicCard, "LEGENDARY", legendaryCard);
        assertSame(byCode.get(expectedCode), drawn);
    }

    @ParameterizedTest
    @CsvSource({ "0.0, true", "0.1999, true", "0.20, false", "0.99, false" })
    void draw_legendaryWithFullArt_givesFullArtBelowChance(double roll, boolean expectFullArt) {
        // Given: full art al 20%
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextDouble()).thenReturn(roll);
        PackDrawer drawer = new PackDrawer(repository, random);

        // When
        Card drawn = drawer.draw(pack("20", slot(1, Map.of(legendary, 100)))).getFirst();

        // Then
        assertSame(expectFullArt ? legendaryFullArt : legendaryCard, drawn);
    }

    @Test
    void draw_cardWithoutFullArt_doesNotRollForIt() {
        // Given
        RandomGenerator random = mock(RandomGenerator.class);
        PackDrawer drawer = new PackDrawer(repository, random);

        // When
        drawer.draw(pack("20", slot(1, Map.of(common, 100))));

        // Then
        verify(random, never()).nextDouble();
    }

    @Test
    void draw_fullArtChanceZero_neverGivesFullArt() {
        // Given
        PackDrawer drawer = new PackDrawer(repository, mock(RandomGenerator.class));

        // When
        Card drawn = drawer.draw(pack("0", slot(1, Map.of(legendary, 100)))).getFirst();

        // Then
        assertSame(legendaryCard, drawn);
        verify(repository, never()).findActiveVariant(any(), any());
    }

    @Test
    void draw_sameRarityInManyPositions_readsCandidatesOnce() {
        // Given
        PackDrawer drawer = new PackDrawer(repository, new Random(42));

        // When
        drawer.draw(pack("20", slot(3, Map.of(common, 100)), slot(2, Map.of(common, 100))));

        // Then
        verify(repository, times(1)).findDrawable(eq(common.getId()), any(), any());
    }

    @Test
    void draw_factionPack_looksOnlyForThatFaction() {
        // Given
        Faction dreams = mock(Faction.class);
        UUID dreamsId = UUID.randomUUID();
        when(dreams.getId()).thenReturn(dreamsId);
        Pack pack = pack("20", slot(1, Map.of(common, 100)));
        when(pack.getFaction()).thenReturn(dreams);
        PackDrawer drawer = new PackDrawer(repository, new Random(42));

        // When
        drawer.draw(pack);

        // Then
        verify(repository).findDrawable(eq(common.getId()), eq(dreamsId), isNull());
    }

    @Test
    void draw_noCardsOfRarity_throwsIllegalStateException() {
        // Given
        when(repository.findDrawable(eq(epic.getId()), any(), any())).thenReturn(List.of());
        PackDrawer drawer = new PackDrawer(repository, new Random(42));
        Pack pack = pack("20", slot(1, Map.of(epic, 100)));

        // When / Then
        assertThrows(IllegalStateException.class, () -> drawer.draw(pack));
    }

    @Test
    void draw_rarityWithManyCards_picksEachWithSameChance() {
        // Given: 4 Comuni diverse, 20.000 estrazioni
        List<Card> commons = List.of(card("C-1"), card("C-2"), card("C-3"), card("C-4"));
        when(repository.findDrawable(eq(common.getId()), any(), any())).thenReturn(commons);
        PackDrawer drawer = new PackDrawer(repository, new Random(7));
        int draws = 20_000;

        // When
        List<Card> drawn = drawer.draw(pack("0", slot(draws, Map.of(common, 100))));

        // Then: ognuna circa il 25%
        for (Card commonOption : commons) {
            long count = drawn.stream().filter(card -> card == commonOption).count();
            assertTrue(Math.abs(count / (double) draws - 0.25) < 0.01, "Uscita " + count + " volte");
        }
    }

    @Test
    void draw_specialSlotManyTimes_followsWeights() {
        // Given: 50.000 slot speciali con pesi 70 / 25 / 5
        PackDrawer drawer = new PackDrawer(repository, new Random(11));
        int draws = 50_000;
        Map<Rarity, Integer> weights = new LinkedHashMap<>();
        weights.put(rare, 70);
        weights.put(epic, 25);
        weights.put(legendary, 5);

        // When
        List<Card> drawn = drawer.draw(pack("0", slot(draws, weights)));

        // Then
        assertShare(drawn, rareCard, 0.70);
        assertShare(drawn, epicCard, 0.25);
        assertShare(drawn, legendaryCard, 0.05);
    }

    @Test
    void draw_starterPack_neverGoesOverMaxCopies() {
        // Given: set iniziale con 4 Comuni da pescare tra 2 carte, al massimo 2 copie per carta
        Card first = card("C-1");
        Card second = card("C-2");
        when(repository.findDrawable(eq(common.getId()), any(), any())).thenReturn(List.of(first, second));
        Pack starter = pack("20", slot(4, Map.of(common, 100)));
        when(starter.getPackType()).thenReturn(PackType.STARTER);
        PackDrawer drawer = new PackDrawer(repository, new Random(42));

        // When
        List<Card> drawn = drawer.draw(starter);

        // Then: per forza 2 copie di ognuna
        assertEquals(2, drawn.stream().filter(card -> card == first).count());
        assertEquals(2, drawn.stream().filter(card -> card == second).count());
    }

    @Test
    void draw_starterPackWithTooFewCards_throwsIllegalStateException() {
        // Given: 5 Comuni da pescare tra 2 carte, ma al massimo 2 copie per carta
        List<Card> twoCommons = List.of(card("C-1"), card("C-2"));
        when(repository.findDrawable(eq(common.getId()), any(), any())).thenReturn(twoCommons);
        Pack starter = pack("20", slot(5, Map.of(common, 100)));
        when(starter.getPackType()).thenReturn(PackType.STARTER);
        PackDrawer drawer = new PackDrawer(repository, new Random(42));

        // When / Then
        assertThrows(IllegalStateException.class, () -> drawer.draw(starter));
    }

    @Test
    void draw_shopPack_canGoOverMaxCopies() {
        // Given: nei pacchetti del negozio il limite non c'è
        Pack shopPack = pack("20", slot(5, Map.of(common, 100)));
        when(shopPack.getPackType()).thenReturn(PackType.SHOP);
        PackDrawer drawer = new PackDrawer(repository, new Random(42));

        // When
        List<Card> drawn = drawer.draw(shopPack);

        // Then: 5 copie dell'unica Comune
        assertEquals(5, drawn.stream().filter(card -> card == commonCard).count());
    }

    private static void assertShare(List<Card> drawn, Card card, double expected) {
        double share = drawn.stream().filter(drawnCard -> drawnCard == card).count() / (double) drawn.size();
        assertTrue(Math.abs(share - expected) < 0.01, card.getCode() + ": " + share);
    }

    private PackSlot specialSlot() {
        return slot(1, Map.of(rare, 70, epic, 25, legendary, 5));
    }

    private static Pack pack(String fullArtChance, PackSlot... slots) {
        Pack pack = mock(Pack.class, withSettings().stubOnly());
        when(pack.getName()).thenReturn("Pacchetto finto");
        when(pack.getFullArtChance()).thenReturn(new BigDecimal(fullArtChance));
        when(pack.getSlots()).thenReturn(List.of(slots));
        return pack;
    }

    private static PackSlot slot(int cardCount, Map<Rarity, Integer> weights) {
        List<PackSlotRarity> rarities = new ArrayList<>();
        weights.forEach((rarity, weight) -> {
            PackSlotRarity slotRarity = mock(PackSlotRarity.class, withSettings().stubOnly());
            when(slotRarity.getRarity()).thenReturn(rarity);
            when(slotRarity.getWeight()).thenReturn(weight);
            rarities.add(slotRarity);
        });
        PackSlot slot = mock(PackSlot.class, withSettings().stubOnly());
        when(slot.getCardCount()).thenReturn(cardCount);
        when(slot.getRarities()).thenReturn(rarities);
        return slot;
    }

    private static Rarity rarity(String code, int sortOrder) {
        Rarity rarity = mock(Rarity.class, withSettings().stubOnly());
        when(rarity.getId()).thenReturn(UUID.randomUUID());
        when(rarity.getCode()).thenReturn(code);
        when(rarity.getSortOrder()).thenReturn(sortOrder);
        when(rarity.getMaxCopies()).thenReturn(code.equals("LEGENDARY") ? 1 : 2);
        return rarity;
    }

    private static Card card(String code) {
        Card card = mock(Card.class, withSettings().stubOnly());
        when(card.getId()).thenReturn(UUID.randomUUID());
        when(card.getCode()).thenReturn(code);
        return card;
    }
}
