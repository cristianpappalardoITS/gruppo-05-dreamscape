package it.somnia.dreamscape.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Controlla i dati del regolamento (V100), il catalogo vero del set base (V102) e i dati finti
// (V900-V903), che nei test si caricano come nel profilo dev.
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
    void baseSet_eachFactionHas33CardsSplitByRarity() {
        // Given: il catalogo di carte/compositore/carte.csv, 15 Comuni, 8 Rare, 6 Epiche e 4 Leggendarie per fazione

        // When
        List<String> counts = jdbcTemplate.queryForList(
                "SELECT f.code || ' ' || r.code || ' ' || count(*) FROM cards c "
                        + "JOIN card_sets s ON s.id = c.card_set_id JOIN factions f ON f.id = c.faction_id "
                        + "JOIN rarities r ON r.id = c.rarity_id "
                        + "WHERE s.code = 'BASE' AND c.base_card_id IS NULL AND c.is_active "
                        + "GROUP BY f.code, r.code, r.sort_order ORDER BY f.code, r.sort_order",
                String.class);

        // Then
        assertEquals(List.of(
                "DREAMS COMMON 15", "DREAMS RARE 8", "DREAMS EPIC 6", "DREAMS LEGENDARY 4",
                "MEMORIES COMMON 15", "MEMORIES RARE 8", "MEMORIES EPIC 6", "MEMORIES LEGENDARY 4",
                "NIGHTMARES COMMON 15", "NIGHTMARES RARE 8", "NIGHTMARES EPIC 6", "NIGHTMARES LEGENDARY 4"),
                counts);
    }

    @Test
    void baseSet_eachLegendaryHasItsFullArt() {
        // Given: le 12 Leggendarie del set base hanno tutte una full art (decisione 025)

        // When
        List<String> fullArts = jdbcTemplate.queryForList(
                "SELECT v.code FROM cards v JOIN cards b ON b.id = v.base_card_id "
                        + "JOIN rarities r ON r.id = b.rarity_id JOIN card_sets s ON s.id = v.card_set_id "
                        + "WHERE v.variant_type = 'FULL_ART' AND r.code = 'LEGENDARY' AND s.code = 'BASE' "
                        + "ORDER BY v.code",
                String.class);

        // Then
        assertEquals(List.of("BASE-004-FA", "BASE-008-FA", "BASE-012-FA", "BASE-034-FA", "BASE-040-FA",
                "BASE-041-FA", "BASE-063-FA", "BASE-069-FA", "BASE-070-FA", "BASE-092-FA", "BASE-098-FA",
                "BASE-099-FA"), fullArts);
    }

    @Test
    void baseSet_effectsHaveTheValueOnlyWhenTheEffectNeedsIt() {
        // Given: 84 carte con un effetto e 15 senza; la X c'è solo per gli effetti con has_value

        // When
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT count(*) AS effects, count(DISTINCT ce.card_id) AS cards, "
                        + "count(*) FILTER (WHERE e.has_value <> (ce.effect_value IS NOT NULL)) AS wrong_values "
                        + "FROM card_effects ce JOIN effects e ON e.id = ce.effect_id "
                        + "JOIN cards c ON c.id = ce.card_id JOIN card_sets s ON s.id = c.card_set_id "
                        + "WHERE s.code = 'BASE'");

        // Then
        assertEquals(84, ((Number) row.get("effects")).intValue());
        assertEquals(84, ((Number) row.get("cards")).intValue());
        assertEquals(0, ((Number) row.get("wrong_values")).intValue());
    }

    @Test
    void baseSet_eachFactionLordIsItsLegendary() {
        // Given: i Lord sono Il Sogno Lucido, L'Uomo Senza Volto e La Custode del Tempo Fermo

        // When
        List<String> lords = jdbcTemplate.queryForList(
                "SELECT f.code || '=' || c.code FROM factions f JOIN cards c ON c.id = f.lord_card_id "
                        + "JOIN rarities r ON r.id = c.rarity_id "
                        + "WHERE c.faction_id = f.id AND r.code = 'LEGENDARY' AND c.base_card_id IS NULL ORDER BY f.code",
                String.class);

        // Then
        assertEquals(List.of("DREAMS=BASE-004", "MEMORIES=BASE-012", "NIGHTMARES=BASE-008"), lords);
    }

    @Test
    void baseSet_everyImageUrlHasItsWebpFile() {
        // Given: gli URL /images/cards/... del set base, con le copie create da carte/compositore/crea_webp.py

        // When
        List<String> urls = jdbcTemplate.queryForList(
                "SELECT unnest(ARRAY[c.illustration_url, c.illustration_thumb_url]) FROM cards c "
                        + "JOIN card_sets s ON s.id = c.card_set_id WHERE s.code = 'BASE'",
                String.class);

        // Then: 99 carte e 12 full art, due copie ciascuna, tutte nel backend
        assertEquals(222, urls.size());
        for (String url : urls) {
            assertTrue(new ClassPathResource(url.substring(1)).exists(), "Manca il file di " + url);
        }
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
                        + "WHERE v.variant_type = 'FULL_ART' AND r.code = 'LEGENDARY' AND v.code LIKE 'TEST-%' "
                        + "ORDER BY v.code",
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
