package it.somnia.dreamscape.api.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import it.somnia.dreamscape.api.common.exception.ApiException;
import it.somnia.dreamscape.api.trade.dto.request.TradeCardRequest;
import it.somnia.dreamscape.api.trade.dto.request.TradeRequest;
import it.somnia.dreamscape.api.trade.service.TradeExceptions.CardsLockedInTradeException;
import it.somnia.dreamscape.api.trade.service.TradeService;

// Due operazioni nello stesso momento sulle stesse copie (consegna della #93): due proposte con l'unica copia,
// oppure un'accettazione e una proposta di chi accetta. Niente @Transactional: ogni proposta deve avere la sua transazione, come due richieste vere.
// Per questo il test usa utenti suoi e li cancella alla fine
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TradeConcurrencyTest {

    private static final String PROPOSER = "scambio_concorrenza_a";
    private static final String RECIPIENT = "scambio_concorrenza_b";

    @Autowired
    private TradeService tradeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID proposerId;
    private UUID recipientId;
    private UUID offeredCardId;
    private UUID requestedCardId;

    @BeforeEach
    void createTwoFriendsWithCards() {
        proposerId = createUser(PROPOSER);
        recipientId = createUser(RECIPIENT);
        jdbcTemplate.update("""
                INSERT INTO friendships (requester_id, addressee_id, status, accepted_at)
                VALUES (?, ?, 'ACCEPTED', now())""", proposerId, recipientId);
        offeredCardId = jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = 'TEST-001'", UUID.class);
        requestedCardId = jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = 'TEST-004'", UUID.class);
        // Chi propone ha una sola copia; chi riceve abbastanza copie per due scambi
        jdbcTemplate.update("INSERT INTO user_cards (user_id, card_id, quantity) VALUES (?, ?, 1)", proposerId,
                offeredCardId);
        jdbcTemplate.update("INSERT INTO user_cards (user_id, card_id, quantity) VALUES (?, ?, 2)", recipientId,
                requestedCardId);
    }

    @AfterEach
    void deleteUsers() {
        // Gli scambi non si cancellano con gli utenti (lo storico resta): prima loro, poi gli utenti con
        // collezione e amicizia (ON DELETE CASCADE)
        jdbcTemplate.update("DELETE FROM trades WHERE proposer_id IN (?, ?) OR recipient_id IN (?, ?)", proposerId,
                recipientId, proposerId, recipientId);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", proposerId, recipientId);
    }

    @Test
    void proposeTrade_twoAtTheSameTimeWithTheOnlyCopy_onlyOneIsCreated() throws Exception {
        // Given
        TradeRequest request = new TradeRequest(recipientId, null,
                List.of(new TradeCardRequest(offeredCardId, 1)), List.of(new TradeCardRequest(requestedCardId, 1)));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<?>> results = new ArrayList<>();

        // When: le due proposte partono insieme
        for (int i = 0; i < 2; i++) {
            results.add(executor.submit(() -> {
                start.await();
                return tradeService.proposeTrade(proposerId, request);
            }));
        }
        start.countDown();
        int created = 0;
        List<Throwable> failures = new ArrayList<>();
        for (Future<?> result : results) {
            try {
                result.get(30, TimeUnit.SECONDS);
                created++;
            } catch (ExecutionException exception) {
                failures.add(exception.getCause());
            }
        }
        executor.shutdown();

        // Then: uno scambio solo, l'altra proposta trova la copia già offerta
        assertEquals(1, created);
        assertEquals(1, failures.size());
        assertInstanceOf(CardsLockedInTradeException.class, failures.getFirst());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE proposer_id = ? AND status = 'PENDING'", Integer.class, proposerId));
    }

    @Test
    void acceptAndProposeAtTheSameTime_withTheSameCopies_onlyOneSucceeds() throws Exception {
        // Given: lo scambio chiede le 2 copie di chi riceve; lui, intanto, ne offre 1 in uno scambio nuovo
        UUID otherCardId = jdbcTemplate.queryForObject("SELECT id FROM cards WHERE code = 'TEST-002'", UUID.class);
        jdbcTemplate.update("INSERT INTO user_cards (user_id, card_id, quantity) VALUES (?, ?, 1)", proposerId,
                otherCardId);
        UUID tradeId = tradeService.proposeTrade(proposerId, new TradeRequest(recipientId, null,
                List.of(new TradeCardRequest(offeredCardId, 1)), List.of(new TradeCardRequest(requestedCardId, 2))))
                .id();
        TradeRequest counterProposal = new TradeRequest(proposerId, null,
                List.of(new TradeCardRequest(requestedCardId, 1)), List.of(new TradeCardRequest(otherCardId, 1)));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // When
        Future<?> accept = executor.submit(() -> {
            start.await();
            return tradeService.acceptTrade(recipientId, tradeId);
        });
        Future<?> propose = executor.submit(() -> {
            start.await();
            return tradeService.proposeTrade(recipientId, counterProposal);
        });
        start.countDown();
        int succeeded = 0;
        for (Future<?> result : List.of(accept, propose)) {
            try {
                result.get(30, TimeUnit.SECONDS);
                succeeded++;
            } catch (ExecutionException exception) {
                // La seconda trova le copie già spostate o già offerte: un 409 previsto, nessun errore inatteso
                assertInstanceOf(ApiException.class, exception.getCause());
            }
        }
        executor.shutdown();

        // Then: una sola delle due, e nessuna copia persa o duplicata
        assertEquals(1, succeeded);
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM user_cards WHERE card_id = ? AND user_id IN (?, ?)",
                Integer.class, requestedCardId, proposerId, recipientId));
    }

    private UUID createUser(String username) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (username, email, password_hash, credits, privacy_accepted_at, terms_accepted_at)
                VALUES (?, ?, 'non-usata', 500, now(), now()) RETURNING id""",
                UUID.class, username, username + "@example.test");
    }
}
