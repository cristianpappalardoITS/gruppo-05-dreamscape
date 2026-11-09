package it.somnia.dreamscape.engine.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.FactionAdvantage;
import it.somnia.dreamscape.engine.input.GameRules;
import it.somnia.dreamscape.engine.log.AfterRound;
import it.somnia.dreamscape.engine.log.Attack;
import it.somnia.dreamscape.engine.log.CardDied;
import it.somnia.dreamscape.engine.log.CardPlayed;
import it.somnia.dreamscape.engine.log.GameEvent;
import it.somnia.dreamscape.engine.log.GameOver;
import it.somnia.dreamscape.engine.log.PlayerDamage;
import it.somnia.dreamscape.engine.log.RoundLog;
import it.somnia.dreamscape.engine.log.RoundOutcome;
import it.somnia.dreamscape.engine.log.SideState;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class BattleEngineTest {

    // Il giro dei vantaggi della decisione 008: Dreams > Memories > Nightmares > Dreams, +1 ATT
    private static final List<FactionAdvantage> ADVANTAGES = List.of(
            new FactionAdvantage(Faction.DREAMS, Faction.MEMORIES, 1, 0),
            new FactionAdvantage(Faction.MEMORIES, Faction.NIGHTMARES, 1, 0),
            new FactionAdvantage(Faction.NIGHTMARES, Faction.DREAMS, 1, 0));
    private static final GameRules RULES = new GameRules(20, 60, ADVANTAGES);

    // Le due carte del round d'esempio del regolamento (schema.md)
    private static final CardSnapshot LIGHTHOUSE = card("Il Faro nella Nebbia", 4, 7, Faction.DREAMS, 1);
    private static final CardSnapshot FACELESS_MAN = card("L'Uomo Senza Volto", 8, 5, Faction.NIGHTMARES, 1);

    private final BattleEngine engine = new BattleEngine();

    @Test
    void simulate_regulationExample_logsRoundInOrder() {
        // When
        BattleResult result = engine.simulate(deck(LIGHTHOUSE), deck(FACELESS_MAN), RULES, 42L, true);

        // Then: l'Uomo (8 + 1) uccide il Faro (DIF 7), il giocatore subisce 9 − 7 = 2
        Attack lighthouseHit = new Attack(Side.CHALLENGER, LIGHTHOUSE.cardId(), 4, 0, 0, 4, 5, 1);
        Attack facelessHit = new Attack(Side.OPPONENT, FACELESS_MAN.cardId(), 8, 1, 0, 9, 7, -2);
        boolean challengerFirst = result.coinTossWinner() == Side.CHALLENGER;
        List<GameEvent> expected = List.of(
                new CardPlayed(Side.CHALLENGER, LIGHTHOUSE.cardId(), 4, 7),
                new CardPlayed(Side.OPPONENT, FACELESS_MAN.cardId(), 8, 5),
                challengerFirst ? lighthouseHit : facelessHit,
                challengerFirst ? facelessHit : lighthouseHit,
                new CardDied(Side.CHALLENGER, LIGHTHOUSE.cardId()),
                new PlayerDamage(Side.CHALLENGER, 2, 20, 18),
                new GameOver(Side.OPPONENT, EndReason.DECK_EXHAUSTED));
        RoundLog round = result.log().rounds().getFirst();
        assertEquals(expected, round.events());
        assertEquals(RoundOutcome.CHALLENGER_CARD_DIED, round.outcome());
    }

    @Test
    void simulate_regulationExample_savesStateAfterRound() {
        // When
        BattleResult result = engine.simulate(deck(LIGHTHOUSE), deck(FACELESS_MAN), RULES, 42L, true);

        // Then
        AfterRound expected = new AfterRound(
                new SideState(null, null, null, 18, 0, 1),
                new SideState(FACELESS_MAN.cardId(), 8, 1, 20, 0, 0));
        assertEquals(expected, result.log().rounds().getFirst().afterRound());
    }

    @Test
    void simulate_cardPlayedAndDied_listChallengerFirstWhateverTheCoin() {
        // Given: un seed con cui la moneta la vince OPPONENT, come nell'esempio di schema.md
        CardSnapshot twin = card("Gemello", 5, 5, Faction.DREAMS, 1);
        long seed = seedWithCoinWinner(Side.OPPONENT, deck(twin), deck(twin));

        // When
        List<GameEvent> events = engine.simulate(deck(twin), deck(twin), RULES, seed, true)
                .log().rounds().getFirst().events();

        // Then
        assertEquals(Side.CHALLENGER, ((CardPlayed) events.get(0)).side());
        assertEquals(Side.OPPONENT, ((CardPlayed) events.get(1)).side());
        assertEquals(Side.CHALLENGER, ((CardDied) events.get(4)).side());
        assertEquals(Side.OPPONENT, ((CardDied) events.get(5)).side());
    }

    @Test
    void simulate_anyCoin_listsCoinWinnerAttackFirst() {
        for (Side coinWinner : Side.values()) {
            // Given
            long seed = seedWithCoinWinner(coinWinner, deck(LIGHTHOUSE), deck(FACELESS_MAN));

            // When
            List<GameEvent> events = engine.simulate(deck(LIGHTHOUSE), deck(FACELESS_MAN), RULES, seed, true)
                    .log().rounds().getFirst().events();

            // Then
            assertEquals(coinWinner, ((Attack) events.get(2)).side());
        }
    }

    @Test
    void simulate_noFactionAdvantage_addsNoBonus() {
        // Given: Memories batte Nightmares, non Dreams
        CardSnapshot memory = card("Ricordo", 3, 9, Faction.MEMORIES, 1);

        // When
        List<GameEvent> events = engine.simulate(deck(memory), deck(LIGHTHOUSE), RULES, 42L, true)
                .log().rounds().getFirst().events();

        // Then: il Faro (Dreams) ha +1 contro Memories, il Ricordo nessun bonus
        Attack memoryHit = findAttack(events, Side.CHALLENGER);
        Attack lighthouseHit = findAttack(events, Side.OPPONENT);
        assertEquals(0, memoryHit.factionBonus());
        assertEquals(1, lighthouseHit.factionBonus());
        assertEquals(5, lighthouseHit.damage());
    }

    @Test
    void simulate_creatureSurvives_keepsWoundsInNextRounds() {
        // Given: stessa fazione, nessun bonus
        CardSnapshot wall = card("Muro", 2, 10, Faction.DREAMS, 1);

        // When
        BattleResult result = engine.simulate(deck(LIGHTHOUSE), deck(wall), RULES, 42L, true);

        // Then: il Muro perde 4 a round (10, 6, 2, morto), il Faro 2 (7, 5, 3, 1)
        List<RoundLog> rounds = result.log().rounds();
        assertEquals(3, rounds.size());
        assertEquals(5, rounds.get(0).afterRound().challenger().defense());
        assertEquals(6, rounds.get(0).afterRound().opponent().defense());
        assertEquals(3, rounds.get(1).afterRound().challenger().defense());
        assertEquals(2, rounds.get(1).afterRound().opponent().defense());
        assertEquals(RoundOutcome.OPPONENT_CARD_DIED, rounds.get(2).outcome());
        assertTrue(rounds.stream().allMatch(round -> round.clash() == 1));
    }

    @Test
    void simulate_survivorDamagedEarlier_usesDefenseBeforeTheHitForPlayerDamage() {
        // Given: il Muro muore al terzo round con 2 di difesa rimasta
        CardSnapshot wall = card("Muro", 2, 10, Faction.DREAMS, 1);

        // When
        BattleResult result = engine.simulate(deck(LIGHTHOUSE), deck(wall), RULES, 42L, true);

        // Then: 4 − 2 = 2 danni al giocatore, non 4 − 10
        List<GameEvent> lastEvents = result.log().rounds().getLast().events();
        assertTrue(lastEvents.contains(new PlayerDamage(Side.OPPONENT, 2, 20, 18)));
    }

    @Test
    void simulate_bothCreaturesDie_damagesNoPlayer() {
        // Given
        CardSnapshot twin = card("Gemello", 5, 5, Faction.DREAMS, 2);

        // When
        BattleResult result = engine.simulate(deck(twin), deck(twin), RULES, 42L, true);

        // Then
        RoundLog first = result.log().rounds().getFirst();
        assertEquals(RoundOutcome.BOTH_DIED, first.outcome());
        assertTrue(first.events().stream().noneMatch(PlayerDamage.class::isInstance));
        assertEquals(20, first.afterRound().challenger().lifePoints());
        assertEquals(20, first.afterRound().opponent().lifePoints());
    }

    @Test
    void simulate_attackEqualToDefense_logsZeroPlayerDamage() {
        // Given
        CardSnapshot killer = card("Sicario", 7, 20, Faction.DREAMS, 1);
        CardSnapshot victim = card("Vittima", 3, 7, Faction.DREAMS, 2);

        // When
        List<GameEvent> events = engine.simulate(deck(killer), deck(victim), RULES, 42L, true)
                .log().rounds().getFirst().events();

        // Then
        assertTrue(events.contains(new PlayerDamage(Side.OPPONENT, 0, 20, 20)));
    }

    @Test
    void simulate_newCreaturePlayed_startsNewClash() {
        // Given
        CardSnapshot killer = card("Sicario", 10, 20, Faction.DREAMS, 1);
        CardSnapshot weak = card("Debole", 1, 1, Faction.DREAMS, 3);

        // When
        BattleResult result = engine.simulate(deck(killer), deck(weak), RULES, 42L, true);

        // Then: nel round 2 gioca solo OPPONENT e comincia il secondo scontro
        RoundLog second = result.log().rounds().get(1);
        assertEquals(2, second.clash());
        assertEquals(new CardPlayed(Side.OPPONENT, weak.cardId(), 1, 1), second.events().getFirst());
        assertInstanceOf(Attack.class, second.events().get(1));
        assertEquals(18, second.afterRound().challenger().defense());
    }

    @Test
    void simulate_lifePointsReachZero_endsWithLifePoints() {
        // Given: ogni round il Sicario toglie 10 − 1 = 9 punti vita
        CardSnapshot killer = card("Sicario", 10, 20, Faction.DREAMS, 1);
        CardSnapshot weak = card("Debole", 1, 1, Faction.DREAMS, 5);

        // When
        BattleResult result = engine.simulate(deck(killer), deck(weak), RULES, 42L, true);

        // Then: 20, 11, 2, 0
        assertEquals(Side.CHALLENGER, result.winner());
        assertEquals(EndReason.LIFE_POINTS, result.endReason());
        assertEquals(3, result.totalRounds());
        assertEquals(0, result.opponentLifePoints());
    }

    @Test
    void simulate_damageAboveLifePoints_stopsLifePointsAtZero() {
        // Given
        CardSnapshot killer = card("Sicario", 10, 20, Faction.DREAMS, 1);
        CardSnapshot weak = card("Debole", 1, 1, Faction.DREAMS, 5);

        // When
        BattleResult result = engine.simulate(deck(killer), deck(weak), RULES, 42L, true);

        // Then
        assertTrue(result.log().rounds().getLast().events().contains(new PlayerDamage(Side.OPPONENT, 9, 2, 0)));
    }

    @Test
    void simulate_lifePointsAndDeckBothOver_givesLifePointsPrecedence() {
        // Given: l'avversario ha una sola carta e 9 punti vita, il Sicario ne toglie 9
        CardSnapshot killer = card("Sicario", 10, 20, Faction.DREAMS, 1);
        CardSnapshot weak = card("Debole", 1, 1, Faction.DREAMS, 1);
        GameRules lowLife = new GameRules(9, 60, ADVANTAGES);

        // When
        BattleResult result = engine.simulate(deck(killer), deck(weak), lowLife, 42L, true);

        // Then
        assertEquals(EndReason.LIFE_POINTS, result.endReason());
        assertEquals(Side.CHALLENGER, result.winner());
    }

    @Test
    void simulate_oneDeckExhausted_givesWinToOtherSide() {
        // When
        BattleResult result = engine.simulate(deck(LIGHTHOUSE), deck(FACELESS_MAN), RULES, 42L, true);

        // Then
        assertEquals(Side.OPPONENT, result.winner());
        assertEquals(EndReason.DECK_EXHAUSTED, result.endReason());
        assertEquals(1, result.totalRounds());
    }

    @Test
    void simulate_bothDecksExhaustedInSameRound_endsInDraw() {
        // Given
        CardSnapshot twin = card("Gemello", 5, 5, Faction.DREAMS, 1);

        // When
        BattleResult result = engine.simulate(deck(twin), deck(twin), RULES, 42L, true);

        // Then
        assertTrue(result.isDraw());
        assertEquals(EndReason.DECK_EXHAUSTED, result.endReason());
    }

    @Test
    void simulate_blockedClash_endsInDrawAtRoundLimit() {
        // Given: due creature con attacco 0 non si fanno niente
        CardSnapshot harmless = card("Innocuo", 0, 5, Faction.DREAMS, 1);
        GameRules fiveRounds = new GameRules(20, 5, ADVANTAGES);

        // When
        BattleResult result = engine.simulate(deck(harmless), deck(harmless), fiveRounds, 42L, true);

        // Then
        assertTrue(result.isDraw());
        assertEquals(EndReason.ROUND_LIMIT, result.endReason());
        assertEquals(5, result.totalRounds());
        assertEquals(5, result.log().rounds().size());
    }

    @Test
    void simulate_gameEnds_logsGameOverAsLastEvent() {
        // Given
        CardSnapshot harmless = card("Innocuo", 0, 5, Faction.DREAMS, 1);
        GameRules fiveRounds = new GameRules(20, 5, ADVANTAGES);

        // When
        BattleResult result = engine.simulate(deck(harmless), deck(harmless), fiveRounds, 42L, true);

        // Then
        List<RoundLog> rounds = result.log().rounds();
        assertEquals(new GameOver(null, EndReason.ROUND_LIMIT), rounds.getLast().events().getLast());
        assertTrue(rounds.subList(0, rounds.size() - 1).stream()
                .flatMap(round -> round.events().stream())
                .noneMatch(GameOver.class::isInstance));
    }

    @Test
    void simulate_sameSeed_playsSameMatch() {
        // When
        BattleResult first = engine.simulate(mixedDeck(), mixedDeck(), RULES, 7L, true);
        BattleResult second = engine.simulate(mixedDeck(), mixedDeck(), RULES, 7L, true);

        // Then
        assertEquals(first, second);
    }

    @Test
    void simulate_logOff_returnsSameResultWithoutLog() {
        // When
        BattleResult withLog = engine.simulate(mixedDeck(), mixedDeck(), RULES, 7L, true);
        BattleResult withoutLog = engine.simulate(mixedDeck(), mixedDeck(), RULES, 7L, false);

        // Then
        assertNull(withoutLog.log());
        assertEquals(withLog.winner(), withoutLog.winner());
        assertEquals(withLog.endReason(), withoutLog.endReason());
        assertEquals(withLog.totalRounds(), withoutLog.totalRounds());
        assertEquals(withLog.challengerLifePoints(), withoutLog.challengerLifePoints());
        assertEquals(withLog.opponentLifePoints(), withoutLog.opponentLifePoints());
        assertEquals(withLog.coinTossWinner(), withoutLog.coinTossWinner());
    }

    @Test
    void simulate_logOn_writesEngineVersionAndCoin() {
        // When
        BattleResult result = engine.simulate(mixedDeck(), mixedDeck(), RULES, 7L, true);

        // Then
        assertEquals(BattleEngine.ENGINE_VERSION, result.log().engineVersion());
        assertEquals(result.coinTossWinner(), result.log().coinTossWinner());
    }

    // Gli effetti arrivano con la #86: fino ad allora una carta con effetto gioca come una senza
    @Test
    void simulate_cardWithEffect_ignoresEffectForNow() {
        // Given
        CardSnapshot withEffect = new CardSnapshot(FACELESS_MAN.cardId(), FACELESS_MAN.name(), 8, 5,
                Faction.NIGHTMARES, 1, List.of(new CardEffect("WEAKENING", 2)));

        // When
        BattleResult plain = engine.simulate(deck(LIGHTHOUSE), deck(FACELESS_MAN), RULES, 42L, true);
        BattleResult withIgnoredEffect = engine.simulate(deck(LIGHTHOUSE), deck(withEffect), RULES, 42L, true);

        // Then
        assertEquals(plain, withIgnoredEffect);
    }

    @Test
    void simulate_missingInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> engine.simulate(null, deck(FACELESS_MAN), RULES, 42L, true));
        assertThrows(NullPointerException.class,
                () -> engine.simulate(deck(LIGHTHOUSE), null, RULES, 42L, true));
        assertThrows(NullPointerException.class,
                () -> engine.simulate(deck(LIGHTHOUSE), deck(FACELESS_MAN), null, 42L, true));
    }

    private static CardSnapshot card(String name, int attack, int defense, Faction faction, int quantity) {
        UUID cardId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return new CardSnapshot(cardId, name, attack, defense, faction, quantity, List.of());
    }

    private static DeckSnapshot deck(CardSnapshot... cards) {
        return new DeckSnapshot(List.of(cards));
    }

    private static DeckSnapshot mixedDeck() {
        return deck(
                card("Sognatore", 3, 6, Faction.DREAMS, 2),
                card("Incubo", 5, 3, Faction.NIGHTMARES, 2),
                card("Ricordo", 2, 8, Faction.MEMORIES, 2),
                card("Guardiano", 4, 7, Faction.MEMORIES, 2),
                LIGHTHOUSE,
                FACELESS_MAN);
    }

    private Attack findAttack(List<GameEvent> events, Side side) {
        return events.stream()
                .filter(Attack.class::isInstance)
                .map(Attack.class::cast)
                .filter(attack -> attack.side() == side)
                .findFirst()
                .orElseThrow();
    }

    private long seedWithCoinWinner(Side coinWinner, DeckSnapshot challengerDeck, DeckSnapshot opponentDeck) {
        for (long seed = 0; seed < 100; seed++) {
            if (engine.simulate(challengerDeck, opponentDeck, RULES, seed, false).coinTossWinner() == coinWinner) {
                return seed;
            }
        }
        throw new AssertionError("Nessun seed tra 0 e 99 dà la moneta a " + coinWinner);
    }
}
