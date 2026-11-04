package it.somnia.dreamscape.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.dto.response.CardResponse;
import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.Rarity;
import it.somnia.dreamscape.api.card.repository.CardEffectRepository;
import it.somnia.dreamscape.api.card.repository.CardRepository;
import it.somnia.dreamscape.api.card.repository.RarityRepository;
import it.somnia.dreamscape.api.user.repository.UserRepository;

// Repository e DTO sui dati delle migration (V100 e dati finti)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RepositoryAndDtoTest {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardEffectRepository cardEffectRepository;
    private final RarityRepository rarityRepository;

    @Autowired
    RepositoryAndDtoTest(UserRepository userRepository, CardRepository cardRepository,
            CardEffectRepository cardEffectRepository, RarityRepository rarityRepository) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.cardEffectRepository = cardEffectRepository;
        this.rarityRepository = rarityRepository;
    }

    @Test
    void findByUsernameIgnoreCase_uppercaseUsername_findsTheUser() {
        // Given: anna_test esiste, scritto in minuscolo

        // When
        boolean found = userRepository.findByUsernameIgnoreCase("ANNA_TEST").isPresent();

        // Then
        assertTrue(found);
    }

    @Test
    void existsByEmailIgnoreCase_mixedCaseEmail_returnsTrue() {
        // Given: bruno_test@example.test esiste

        // When
        boolean exists = userRepository.existsByEmailIgnoreCase("Bruno_Test@Example.test");

        // Then
        assertTrue(exists);
    }

    @Test
    void findByBaseCardAndVariantType_legendary_findsItsFullArt() {
        // Given
        Card legendary = cardRepository.findByCode("TEST-026").orElseThrow();

        // When
        Card fullArt = cardRepository.findByBaseCardAndVariantType(legendary, CardVariantType.FULL_ART).orElseThrow();

        // Then
        assertEquals("TEST-026-FA", fullArt.getCode());
    }

    @Test
    void findByBaseCardIsNullAndActiveTrue_testData_excludesTheVersions() {
        // Given: 39 carte normali e 3 full art

        // When
        List<Card> cards = cardRepository.findByBaseCardIsNullAndActiveTrueOrderByCodeAsc();

        // Then
        assertEquals(39, cards.size());
    }

    @Test
    void findAllByOrderBySortOrderAsc_seedRules_returnsCommonToLegendary() {
        // Given: le rarità di V100

        // When
        List<String> codes = rarityRepository.findAllByOrderBySortOrderAsc().stream().map(Rarity::getCode).toList();

        // Then
        assertEquals(List.of("COMMON", "RARE", "EPIC", "LEGENDARY"), codes);
    }

    @Test
    void cardResponseFrom_fullArt_usesItsIdAndTheBaseCardData() {
        // Given: TEST-013-FA, full art di TEST-013, che ha Riposo 3
        Card fullArt = cardRepository.findByCode("TEST-013-FA").orElseThrow();
        Card base = fullArt.getBaseCard();

        // When
        CardResponse response = CardResponse.from(fullArt, cardEffectRepository.findByCard(base).orElse(null));

        // Then
        assertEquals(fullArt.getId(), response.id());
        assertEquals(base.getId(), response.baseCardId());
        assertEquals(CardVariantType.FULL_ART, response.variantType());
        assertEquals("Sognatrice Lucida (prova)", response.name());
        assertEquals(6, response.attack());
        assertEquals("Quando viene giocata: il tuo giocatore guadagna 3 punti vita.", response.effect().text());
    }

    @Test
    void cardResponseFrom_normalCardWithoutEffect_hasNoBaseCardAndNoEffect() {
        // Given: TEST-001 è una Comune senza effetto
        Card card = cardRepository.findByCode("TEST-001").orElseThrow();

        // When
        CardResponse response = CardResponse.from(card, cardEffectRepository.findByCard(card).orElse(null));

        // Then
        assertNull(response.baseCardId());
        assertNull(response.effect());
        assertEquals("DREAMS", response.faction().code());
        assertNull(response.lordTitle());
    }
}
