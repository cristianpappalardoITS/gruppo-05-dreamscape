package it.somnia.dreamscape.api.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import it.somnia.dreamscape.api.TestcontainersConfiguration;
import it.somnia.dreamscape.api.pack.service.PackService;
import it.somnia.dreamscape.api.user.service.UserExceptions.NotEnoughCreditsException;

// Due acquisti dello stesso utente nello stesso momento, con crediti per uno solo (consegna della #78).
// Niente @Transactional: ogni acquisto deve avere la sua transazione, come due richieste vere.
// Per questo il test usa un utente suo e lo cancella alla fine.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PackPurchaseConcurrencyTest {

    private static final String USERNAME = "concorrenza_test";

    @Autowired
    private PackService packService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID packId;

    @BeforeEach
    void createUserWithCreditsForOnePack() {
        userId = jdbcTemplate.queryForObject("""
                INSERT INTO users (username, email, password_hash, credits, privacy_accepted_at, terms_accepted_at)
                VALUES (?, ?, 'non-usata', 500, now(), now()) RETURNING id""",
                UUID.class, USERNAME, USERNAME + "@example.test");
        packId = jdbcTemplate.queryForObject("SELECT id FROM packs WHERE name = 'Pacchetto di prova'", UUID.class);
    }

    @AfterEach
    void deleteUser() {
        // La cancellazione porta via aperture, carte, collezione e movimenti (ON DELETE CASCADE)
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void openPack_twoPurchasesAtOnceWithCreditsForOne_onlyOneSucceeds() throws Exception {
        // Given: due richieste che partono insieme
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> purchase = () -> {
            start.await();
            try {
                packService.openPack(userId, packId, null);
                return "OK";
            } catch (NotEnoughCreditsException exception) {
                return exception.getCode();
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<String>> results = new ArrayList<>();

        // When
        try {
            results.add(executor.submit(purchase));
            results.add(executor.submit(purchase));
            start.countDown();
        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }

        // Then
        List<String> outcomes = new ArrayList<>();
        for (Future<String> result : results) {
            outcomes.add(result.get());
        }
        assertEquals(1, outcomes.stream().filter("OK"::equals).count(), "Esiti: " + outcomes);
        assertEquals(1, outcomes.stream().filter("NOT_ENOUGH_CREDITS"::equals).count(), "Esiti: " + outcomes);
        assertEquals(0, jdbcTemplate.queryForObject("SELECT credits FROM users WHERE id = ?", Integer.class, userId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pack_openings WHERE user_id = ?", Integer.class, userId));
        assertEquals(5, jdbcTemplate.queryForObject(
                "SELECT coalesce(sum(quantity), 0) FROM user_cards WHERE user_id = ?", Integer.class, userId));
    }
}
