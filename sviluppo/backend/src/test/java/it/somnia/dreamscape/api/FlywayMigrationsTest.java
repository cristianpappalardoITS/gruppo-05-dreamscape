package it.somnia.dreamscape.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// Flyway parte all'avvio del contesto su un PostgreSQL 18 vuoto (Testcontainers):
// questi test controllano cosa hanno creato le migration V1-V10.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywayMigrationsTest {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    FlywayMigrationsTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void migrate_emptyDatabase_creates31Tables() {
        // Given: database vuoto, migrato all'avvio

        // When
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'",
                Integer.class);

        // Then
        assertEquals(31, tables);
    }

    @Test
    void migrate_emptyDatabase_insertsBaseGameMode() {
        // Given: database vuoto, migrato all'avvio

        // When
        List<String> codes = jdbcTemplate.queryForList("SELECT code FROM game_modes", String.class);

        // Then
        assertEquals(List.of("BASE"), codes);
    }

    @Test
    void migrate_emptyDatabase_insertsTheTwoMissions() {
        // Given: database vuoto, migrato all'avvio

        // When
        List<String> codes = jdbcTemplate.queryForList("SELECT code FROM missions ORDER BY code", String.class);

        // Then
        assertEquals(List.of("DAILY_PLAY", "WEEKLY_WIN"), codes);
    }

    @Test
    void migrate_emptyDatabase_insertsTheFourLeaguesInOrder() {
        // Given: database vuoto, migrato all'avvio

        // When
        List<String> codes = jdbcTemplate.queryForList("SELECT code FROM leagues ORDER BY min_rating", String.class);

        // Then
        assertEquals(List.of("DORMANT", "DREAMER", "LUCID", "DREAMWEAVER"), codes);
    }
}
