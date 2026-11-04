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

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardEffect;
import it.somnia.dreamscape.api.card.model.CardVariantType;
import it.somnia.dreamscape.api.card.model.EffectTrigger;
import it.somnia.dreamscape.api.card.model.FactionAdvantage;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.model.UserRole;
import jakarta.persistence.EntityManager;

// Legge i dati delle migration (V100 e dati finti) attraverso i model, per controllare
// che le entità corrispondano alle tabelle. Che le colonne esistano lo controlla già
// Hibernate all'avvio (ddl-auto=validate).
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ModelMappingTest {

    private final EntityManager entityManager;

    @Autowired
    ModelMappingTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    void findUser_testUser_readsRoleCreditsAndNoStarterFaction() {
        // Given: anna_test dei dati finti

        // When
        User user = entityManager.createQuery("SELECT u FROM User u WHERE u.username = 'anna_test'", User.class)
                .getSingleResult();

        // Then
        assertEquals(UserRole.PLAYER, user.getRole());
        assertEquals(500, user.getCredits());
        assertEquals(1000, user.getRating());
        assertTrue(user.isEnabled());
        assertNull(user.getStarterFaction());
    }

    @Test
    void findCard_fullArt_readsTheDataFromTheBaseCard() {
        // Given: TEST-013-FA è la full art di TEST-013

        // When
        Card fullArt = entityManager.createQuery("SELECT c FROM Card c WHERE c.code = 'TEST-013-FA'", Card.class)
                .getSingleResult();

        // Then
        assertEquals(CardVariantType.FULL_ART, fullArt.getVariantType());
        assertNull(fullArt.getName());
        assertEquals("Sognatrice Lucida (prova)", fullArt.getBaseCard().getName());
        assertEquals("LEGENDARY", fullArt.getBaseCard().getRarity().getCode());
    }

    @Test
    void findCardEffect_punishmentCard_readsEffectAndValue() {
        // Given: TEST-015 ha Castigo 1

        // When
        CardEffect cardEffect = entityManager.createQuery(
                "SELECT ce FROM CardEffect ce WHERE ce.card.code = 'TEST-015'", CardEffect.class)
                .getSingleResult();

        // Then
        assertEquals("PUNISHMENT", cardEffect.getEffect().getCode());
        assertEquals(EffectTrigger.ON_PLAY, cardEffect.getEffect().getTriggerEvent());
        assertTrue(cardEffect.getEffect().hasValue());
        assertEquals(1, cardEffect.getEffectValue());
    }

    @Test
    void findFactionAdvantages_seedRules_readsTheCycle() {
        // Given: il giro dei vantaggi di V100

        // When
        List<String> pairs = entityManager.createQuery("SELECT a FROM FactionAdvantage a", FactionAdvantage.class)
                .getResultList().stream()
                .map(a -> a.getFaction().getCode() + ">" + a.getTargetFaction().getCode())
                .sorted()
                .toList();

        // Then
        assertEquals(List.of("DREAMS>MEMORIES", "MEMORIES>NIGHTMARES", "NIGHTMARES>DREAMS"), pairs);
    }
}
