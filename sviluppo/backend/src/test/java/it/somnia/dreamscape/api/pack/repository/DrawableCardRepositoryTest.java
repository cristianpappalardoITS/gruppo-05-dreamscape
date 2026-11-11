package it.somnia.dreamscape.api.pack.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.repository.CardRepository;

// Quali carte possono uscire dai pacchetti, con le carte di prova di V900 (7 Comuni per fazione)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class DrawableCardRepositoryTest {

    @Autowired
    private DrawableCardRepository drawableCardRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findDrawable_commonsOfAllFactions_returnsAllActiveCommons() {
        // When
        List<Card> cards = drawableCardRepository.findDrawable(rarityId("COMMON"), null, null);

        // Then
        assertEquals(21, cards.size());
    }

    @Test
    void findDrawable_oneFaction_returnsOnlyThatFaction() {
        // When
        List<Card> cards = drawableCardRepository.findDrawable(rarityId("COMMON"), factionId("DREAMS"), null);

        // Then
        assertEquals(7, cards.size());
        assertTrue(cards.stream().allMatch(card -> card.getFaction().getCode().equals("DREAMS")));
    }

    @Test
    void findDrawable_legendaries_excludesFullArts() {
        // When
        List<Card> cards = drawableCardRepository.findDrawable(rarityId("LEGENDARY"), null, null);

        // Then
        assertEquals(List.of("TEST-013", "TEST-026", "TEST-039"), cards.stream().map(Card::getCode).toList());
    }

    @Test
    void findDrawable_inactiveCard_isExcluded() {
        // Given
        jdbcTemplate.update("UPDATE cards SET is_active = FALSE WHERE code = 'TEST-001'");

        // When
        List<Card> cards = drawableCardRepository.findDrawable(rarityId("COMMON"), factionId("DREAMS"), null);

        // Then
        assertEquals(6, cards.size());
    }

    @Test
    void findDrawable_inactiveCardSetWithoutSetFilter_isExcluded() {
        // Given
        jdbcTemplate.update("UPDATE card_sets SET is_active = FALSE WHERE code = 'TEST'");

        // When
        List<Card> cards = drawableCardRepository.findDrawable(rarityId("COMMON"), null, null);

        // Then
        assertEquals(0, cards.size());
    }

    @Test
    void findDrawable_packWithItsOwnCardSet_usesThatSetEvenIfInactive() {
        // Given: il pacchetto indica l'espansione, quindi vale quella
        jdbcTemplate.update("UPDATE card_sets SET is_active = FALSE WHERE code = 'TEST'");
        UUID testSetId = jdbcTemplate.queryForObject("SELECT id FROM card_sets WHERE code = 'TEST'", UUID.class);

        // When
        List<Card> cards = drawableCardRepository.findDrawable(rarityId("COMMON"), null, testSetId);

        // Then
        assertEquals(21, cards.size());
    }

    @Test
    void findActiveVariant_legendaryWithFullArt_returnsFullArt() {
        // Given
        Card legendary = cardRepository.findByCode("TEST-013").orElseThrow();

        // When / Then
        assertEquals("TEST-013-FA", drawableCardRepository.findActiveVariant(legendary, CardVariantType.FULL_ART)
                .orElseThrow().getCode());
    }

    @Test
    void findActiveVariant_inactiveFullArt_returnsEmpty() {
        // Given
        jdbcTemplate.update("UPDATE cards SET is_active = FALSE WHERE code = 'TEST-013-FA'");
        Card legendary = cardRepository.findByCode("TEST-013").orElseThrow();

        // When / Then
        assertTrue(drawableCardRepository.findActiveVariant(legendary, CardVariantType.FULL_ART).isEmpty());
    }

    private UUID rarityId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM rarities WHERE code = ?", UUID.class, code);
    }

    private UUID factionId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM factions WHERE code = ?", UUID.class, code);
    }
}
