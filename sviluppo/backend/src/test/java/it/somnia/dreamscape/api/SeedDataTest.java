package it.somnia.dreamscape.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Controlla i dati del regolamento (V100) e i dati finti (V900-V902),
// che nei test si caricano come nel profilo dev.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SeedDataTest {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    SeedDataTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void seedRules_insertsTheFourRaritiesInOrder() {
        // Given: database migrato all'avvio

        // When
        List<String> codes = jdbcTemplate.queryForList("SELECT code FROM rarities ORDER BY sort_order", String.class);

        // Then
        assertEquals(List.of("COMMON", "RARE", "EPIC", "LEGENDARY"), codes);
    }

    @Test
    void seedRules_insertsTheFactionAdvantageCycle() {
        // Given: database migrato all'avvio

        // When
        List<String> pairs = jdbcTemplate.queryForList(
                "SELECT f.code || '>' || t.code FROM faction_advantages a "
                        + "JOIN factions f ON f.id = a.faction_id JOIN factions t ON t.id = a.target_faction_id "
                        + "ORDER BY 1",
                String.class);

        // Then
        assertEquals(List.of("DREAMS>MEMORIES", "MEMORIES>NIGHTMARES", "NIGHTMARES>DREAMS"), pairs);
    }

    @Test
    void seedRules_insertsTheNineteenEffects() {
        // Given: database migrato all'avvio

        // When
        Integer effects = jdbcTemplate.queryForObject("SELECT count(*) FROM effects WHERE is_active", Integer.class);

        // Then
        assertEquals(19, effects);
    }

    @Test
    void testData_eachFactionHasEnoughCardsForTheStarterSet() {
        // Given: il set iniziale vuole 14 Comuni e 6 Rare di una fazione, al massimo 2 copie per carta

        // When
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT f.code AS faction, "
                        + "count(*) FILTER (WHERE r.code = 'COMMON') AS commons, "
                        + "count(*) FILTER (WHERE r.code = 'RARE') AS rares "
                        + "FROM cards c JOIN factions f ON f.id = c.faction_id JOIN rarities r ON r.id = c.rarity_id "
                        + "WHERE c.base_card_id IS NULL AND c.is_active GROUP BY f.code");

        // Then
        assertEquals(3, rows.size());
        for (Map<String, Object> row : rows) {
            assertTrue(((Number) row.get("commons")).intValue() >= 7, "Comuni di " + row.get("faction"));
            assertTrue(((Number) row.get("rares")).intValue() >= 3, "Rare di " + row.get("faction"));
        }
    }

    @Test
    void testData_eachTestLegendaryHasItsFullArt() {
        // Given: database migrato all'avvio

        // When
        List<String> fullArts = jdbcTemplate.queryForList(
                "SELECT v.code FROM cards v JOIN cards b ON b.id = v.base_card_id "
                        + "JOIN rarities r ON r.id = b.rarity_id "
                        + "WHERE v.variant_type = 'FULL_ART' AND r.code = 'LEGENDARY' ORDER BY v.code",
                String.class);

        // Then
        assertEquals(List.of("TEST-013-FA", "TEST-026-FA", "TEST-039-FA"), fullArts);
    }

    @Test
    void testData_testUserPasswordMatchesTheReadme() {
        // Given: la password di prova scritta in sviluppo/backend/README.md
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = 'anna_test'", String.class);

        // When
        boolean matches = new BCryptPasswordEncoder().matches("prova-dreamscape", hash);

        // Then
        assertTrue(matches);
    }

    @Test
    void testData_testUsersStartWithTheSignupBonus() {
        // Given: database migrato all'avvio

        // When
        Integer users = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users u JOIN credit_transactions t ON t.user_id = u.id "
                        + "WHERE u.username LIKE '%\\_test' AND u.credits = 500 "
                        + "AND t.type = 'SIGNUP_BONUS' AND t.balance_after = 500",
                Integer.class);

        // Then
        assertEquals(3, users);
    }
}
