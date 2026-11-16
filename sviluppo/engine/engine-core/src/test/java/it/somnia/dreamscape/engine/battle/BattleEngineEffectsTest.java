package it.somnia.dreamscape.engine.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.input.CardEffect;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.EffectCode;
import it.somnia.dreamscape.engine.input.Faction;
import it.somnia.dreamscape.engine.input.FactionAdvantage;
import it.somnia.dreamscape.engine.input.GameRules;
import it.somnia.dreamscape.engine.log.AfterRound;
import it.somnia.dreamscape.engine.log.Attack;
import it.somnia.dreamscape.engine.log.BonusExpired;
import it.somnia.dreamscape.engine.log.CardPlayed;
import it.somnia.dreamscape.engine.log.EffectApplied;
import it.somnia.dreamscape.engine.log.EffectBlocked;
import it.somnia.dreamscape.engine.log.GameEvent;
import it.somnia.dreamscape.engine.log.Phase;
import it.somnia.dreamscape.engine.log.PlayerDamage;
import it.somnia.dreamscape.engine.log.RoundLog;
import it.somnia.dreamscape.engine.log.RoundOutcome;
import it.somnia.dreamscape.engine.log.Stat;
import it.somnia.dreamscape.engine.log.Target;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

// I 19 effetti della decisione 028, uno per volta: mazzi da una carta, così l'ordine non dipende dal seed.
// CHALLENGER gioca sempre la carta "mine", OPPONENT la carta "foe"; stessa fazione se non serve il bonus
class BattleEngineEffectsTest {

    private static final List<FactionAdvantage> ADVANTAGES = List.of(
            new FactionAdvantage(Faction.DREAMS, Faction.MEMORIES, 1, 0),
            new FactionAdvantage(Faction.MEMORIES, Faction.NIGHTMARES, 1, 0),
            new FactionAdvantage(Faction.NIGHTMARES, Faction.DREAMS, 1, 0));
    // 100 punti vita: i danni degli effetti non chiudono la partita prima di quello che si vuole guardare
    private static final GameRules RULES = new GameRules(100, 60, ADVANTAGES);
    private static final long SEED = 42L;

    private final BattleEngine engine = new BattleEngine();

    // ------------------------------------------------------------------ quando viene giocata

    @Test
    void punishment_onPlay_damagesFoeDefenseBeforeTheHit() {
        // Given
        CardSnapshot mine = card("Castigatore", 1, 10, "PUNISHMENT", 3);
        CardSnapshot foe = card("Bersaglio", 1, 5);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then: la difesa scende da 5 a 2 e il colpo parte da 2
        assertContains(events, applied(Phase.START, Side.CHALLENGER, mine, 3, Side.OPPONENT, Target.CARD,
                Stat.DEFENSE, 5, 2, false));
        assertEquals(2, attackOf(events, Side.CHALLENGER).defenseBefore());
    }

    @Test
    void punishment_strongerThanFoeDefense_leavesItAtOne() {
        // Given: Castigo non uccide
        CardSnapshot mine = card("Castigatore", 0, 10, "PUNISHMENT", 9);
        CardSnapshot foe = card("Bersaglio", 1, 5);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then
        assertContains(events, applied(Phase.START, Side.CHALLENGER, mine, 9, Side.OPPONENT, Target.CARD,
                Stat.DEFENSE, 5, 1, false));
    }

    @Test
    void scourge_onPlay_damagesFoePlayer() {
        // Given
        CardSnapshot mine = card("Flagellatore", 1, 10, "SCOURGE", 3);

        // When
        BattleResult result = simulate(mine, card("Bersaglio", 1, 10));

        // Then
        assertContains(events(result, 1), applied(Phase.START, Side.CHALLENGER, mine, 3, Side.OPPONENT,
                Target.PLAYER, Stat.LIFE_POINTS, 100, 97, false));
        assertEquals(97, result.opponentLifePoints());
    }

    @Test
    void rest_onPlay_healsOwnPlayerAboveStartingLifePoints() {
        // Given
        CardSnapshot mine = card("Riposante", 1, 10, "REST", 4);

        // When
        BattleResult result = simulate(mine, card("Bersaglio", 1, 10));

        // Then
        assertContains(events(result, 1), applied(Phase.START, Side.CHALLENGER, mine, 4, Side.CHALLENGER,
                Target.PLAYER, Stat.LIFE_POINTS, 100, 104, false));
        assertEquals(104, result.challengerLifePoints());
    }

    @Test
    void hope_onPlay_addsDefenseConsumedFirstAndRemovesTheRestAtEndOfRound() {
        // Given: Speranza 3, il colpo ne consuma 2 (decisione 029, punto d)
        CardSnapshot mine = card("Speranzosa", 1, 5, "HOPE", 3);
        CardSnapshot foe = card("Bersaglio", 2, 50);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then: 5 → 8, colpo 8 → 6, a fine round sparisce il punto rimasto 6 → 5
        assertContains(events, applied(Phase.START, Side.CHALLENGER, mine, 3, Side.CHALLENGER, Target.CARD,
                Stat.DEFENSE, 5, 8, true));
        assertEquals(8, attackOf(events, Side.OPPONENT).defenseBefore());
        assertContains(events, new BonusExpired(Side.CHALLENGER, mine.cardId(), "HOPE", Stat.DEFENSE, 6, 5));
    }

    @Test
    void hope_fullyConsumedByTheHit_expiresWithoutEvent() {
        // Given
        CardSnapshot mine = card("Speranzosa", 1, 5, "HOPE", 3);
        CardSnapshot foe = card("Bersaglio", 4, 50);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then
        assertFalse(events.stream().anyMatch(BonusExpired.class::isInstance));
    }

    @Test
    void vengeance_withFiveCardsInDiscard_addsAttackOnlyInThatRound() {
        // Given: 6 copie; le prime 5 muoiono contro il Muro, la sesta ha 5 carte negli scarti
        CardSnapshot mine = new CardSnapshot(id("Vendicatore"), "Vendicatore", 1, 1, Faction.DREAMS, 6,
                List.of(new CardEffect("VENGEANCE", 3)));
        CardSnapshot wall = card("Muro", 10, 100);
        GameRules manyLifePoints = new GameRules(1000, 60, ADVANTAGES);

        // When
        BattleResult result = engine.simulate(deck(mine), deck(wall), manyLifePoints, SEED, true);

        // Then
        assertEquals(0, attackOf(events(result, 5), Side.CHALLENGER).attackModifier());
        assertContains(events(result, 6), applied(Phase.START, Side.CHALLENGER, mine, 3, Side.CHALLENGER,
                Target.CARD, Stat.ATTACK, 1, 4, true));
        Attack boostedHit = attackOf(events(result, 6), Side.CHALLENGER);
        assertEquals(3, boostedHit.attackModifier());
        assertEquals(4, boostedHit.damage());
    }

    // ------------------------------------------------------------------ mentre è in campo

    @Test
    void weakening_firstRoundOfClash_lowersFoeAttackThenExpires() {
        // Given
        CardSnapshot mine = card("Bersaglio", 4, 7);
        CardSnapshot foe = card("Indebolitore", 3, 9, "WEAKENING", 2);

        // When
        BattleResult result = simulate(mine, foe);

        // Then: nel primo round il colpo è 4 − 2, dal secondo torna 4
        List<GameEvent> first = events(result, 1);
        assertContains(first, applied(Phase.COMBAT, Side.OPPONENT, foe, 2, Side.CHALLENGER, Target.CARD,
                Stat.ATTACK, 4, 2, true));
        assertEquals(-2, attackOf(first, Side.CHALLENGER).attackModifier());
        assertEquals(2, attackOf(first, Side.CHALLENGER).damage());
        assertContains(first, new BonusExpired(Side.CHALLENGER, mine.cardId(), "WEAKENING", Stat.ATTACK, 2, 4));
        assertEquals(4, attackOf(events(result, 2), Side.CHALLENGER).damage());
    }

    @Test
    void neutrality_blocksFoeFactionBonus() {
        // Given: Nightmares è forte contro Dreams
        CardSnapshot mine = card("Incubo", 4, 10, Faction.NIGHTMARES, null, null);
        CardSnapshot foe = card("Neutrale", 4, 10, Faction.DREAMS, "NEUTRALITY", null);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then
        assertContains(events, new EffectBlocked(Phase.COMBAT, Side.OPPONENT, foe.cardId(), "NEUTRALITY",
                EffectBlocked.FACTION_BONUS));
        assertEquals(0, attackOf(events, Side.CHALLENGER).factionBonus());
        assertEquals(4, attackOf(events, Side.CHALLENGER).damage());
    }

    @Test
    void neutrality_keepsItsOwnFactionBonus() {
        // Given: Dreams è forte contro Memories
        CardSnapshot mine = card("Ricordo", 4, 10, Faction.MEMORIES, null, null);
        CardSnapshot foe = card("Neutrale", 4, 10, Faction.DREAMS, "NEUTRALITY", null);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then
        assertEquals(1, attackOf(events, Side.OPPONENT).factionBonus());
        assertFalse(events.stream().anyMatch(EffectBlocked.class::isInstance));
    }

    @Test
    void immunity_blocksFoeEffectsOnTheCreature() {
        // Given
        CardSnapshot mine = card("Castigatore", 1, 10, "PUNISHMENT", 3);
        CardSnapshot foe = card("Immune", 1, 5, "IMMUNITY", null);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then
        assertContains(events, new EffectBlocked(Phase.START, Side.OPPONENT, foe.cardId(), "IMMUNITY",
                "PUNISHMENT"));
        assertEquals(5, attackOf(events, Side.CHALLENGER).defenseBefore());
    }

    @Test
    void immunity_doesNotProtectThePlayer() {
        // Given: Immunità vale per la creatura, non per i punti vita
        CardSnapshot mine = card("Flagellatore", 1, 10, "SCOURGE", 3);
        CardSnapshot foe = card("Immune", 1, 10, "IMMUNITY", null);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertEquals(97, result.opponentLifePoints());
    }

    @Test
    void nonSequitur_annulsFoeEffect() {
        // Given
        CardSnapshot mine = card("Riposante", 1, 10, "REST", 4);
        CardSnapshot foe = card("Annullatore", 1, 10, "NON_SEQUITUR", null);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertContains(events(result, 1), new EffectBlocked(Phase.START, Side.OPPONENT, foe.cardId(),
                "NON_SEQUITUR", "REST"));
        assertEquals(100, result.challengerLifePoints());
    }

    @Test
    void nonSequitur_againstImmunity_doesNotWork() {
        // Given
        CardSnapshot mine = card("Immune", 1, 10, "IMMUNITY", null);
        CardSnapshot foe = card("Annullatore", 1, 10, "NON_SEQUITUR", null);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then
        assertContains(events, new EffectBlocked(Phase.START, Side.CHALLENGER, mine.cardId(), "IMMUNITY",
                "NON_SEQUITUR"));
    }

    @Test
    void nonSequitur_playedLater_keepsBonusesAlreadyGained() {
        // Given: Intensificazione cresce contro la Preda, poi entra il Non Sequitur
        CardSnapshot mine = card("Crescente", 1, 100, "INTENSIFICATION", 2);
        CardSnapshot prey = card("Preda", 0, 1);
        CardSnapshot annuller = card("Annullatore", 0, 50, "NON_SEQUITUR", null);
        long seed = seedWhere(deck(mine), deck(prey, annuller),
                result -> firstPlayed(result, Side.OPPONENT).equals(prey.cardId()));

        // When
        BattleResult result = engine.simulate(deck(mine), deck(prey, annuller), RULES, seed, true);

        // Then: l'attacco 3 del primo round resta, ma dal secondo non cresce più
        assertEquals(3, afterRound(result, 1).challenger().attack());
        assertContains(events(result, 2), new EffectBlocked(Phase.END, Side.OPPONENT, annuller.cardId(),
                "NON_SEQUITUR", "INTENSIFICATION"));
        assertEquals(3, afterRound(result, 2).challenger().attack());
        assertEquals(3, afterRound(result, 3).challenger().attack());
    }

    @Test
    void nonSequitur_onBothSides_cancelEachOther() {
        // Given
        CardSnapshot mine = card("Annullatore", 1, 10, "NON_SEQUITUR", null);
        CardSnapshot foe = card("Altro Annullatore", 1, 10, "NON_SEQUITUR", null);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then
        assertFalse(events.stream().anyMatch(EffectBlocked.class::isInstance));
    }

    @Test
    void cradle_blocksCombatDamageEvenInTheRoundItDies() {
        // Given: il colpo 9 uccide la Culla (DIF 5): senza Culla il giocatore perderebbe 4 punti vita
        CardSnapshot mine = card("Forte", 9, 10);
        CardSnapshot foe = card("Culla", 1, 5, "CRADLE", null);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        List<GameEvent> events = events(result, 1);
        assertContains(events, new EffectBlocked(Phase.END, Side.OPPONENT, foe.cardId(), "CRADLE",
                EffectBlocked.PLAYER_DAMAGE));
        assertFalse(events.stream().anyMatch(PlayerDamage.class::isInstance));
        assertEquals(100, result.opponentLifePoints());
    }

    @Test
    void cradle_blocksScourge() {
        // Given
        CardSnapshot mine = card("Flagellatore", 1, 10, "SCOURGE", 3);
        CardSnapshot foe = card("Culla", 1, 10, "CRADLE", null);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertContains(events(result, 1), new EffectBlocked(Phase.START, Side.OPPONENT, foe.cardId(), "CRADLE",
                "SCOURGE"));
        assertEquals(100, result.opponentLifePoints());
    }

    @Test
    void cradle_blocksMiasmaAndPureTerror() {
        // Given: Terrore puro uccide la Culla nel primo round
        CardSnapshot terror = card("Terrore", 9, 10, "PURE_TERROR", null);
        CardSnapshot miasma = card("Miasma", 1, 10, "MIASMA", 2);
        CardSnapshot cradle = card("Culla", 1, 5, "CRADLE", null);

        // When
        BattleResult againstTerror = simulate(terror, cradle);
        BattleResult againstMiasma = simulate(miasma, cradle);

        // Then
        assertContains(events(againstTerror, 1), new EffectBlocked(Phase.END, Side.OPPONENT, cradle.cardId(),
                "CRADLE", "PURE_TERROR"));
        assertEquals(100, againstTerror.opponentLifePoints());
        assertContains(events(againstMiasma, 1), new EffectBlocked(Phase.END, Side.OPPONENT, cradle.cardId(),
                "CRADLE", "MIASMA"));
        assertEquals(100, againstMiasma.opponentLifePoints());
    }

    @Test
    void reflection_whenHitInCombat_damagesFoeDefenseAfterTheHits() {
        // Given
        CardSnapshot mine = card("Bersaglio", 2, 10);
        CardSnapshot foe = card("Specchio", 1, 10, "REFLECTION", 3);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then: il colpo dello Specchio porta la difesa a 9, il riflesso a 6
        assertContains(events, applied(Phase.COMBAT, Side.OPPONENT, foe, 3, Side.CHALLENGER, Target.CARD,
                Stat.DEFENSE, 9, 6, false));
        int reflection = indexOf(events, EffectApplied.class);
        assertTrue(reflection > indexOfLast(events, Attack.class));
    }

    @Test
    void reflection_canKill() {
        // Given: decisione 029, punto f
        CardSnapshot mine = card("Fragile", 2, 4);
        CardSnapshot foe = card("Specchio", 1, 10, "REFLECTION", 3);

        // When
        RoundLog round = simulate(mine, foe).log().rounds().getFirst();

        // Then: 4 − 1 − 3 = 0
        assertEquals(RoundOutcome.CHALLENGER_CARD_DIED, round.outcome());
    }

    @Test
    void barrier_cancelsOnlyTheFirstHit() {
        // Given
        CardSnapshot mine = card("Colpitore", 3, 10);
        CardSnapshot foe = card("Barriera", 1, 10, "BARRIER", null);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        List<GameEvent> first = events(result, 1);
        assertContains(first, new EffectBlocked(Phase.COMBAT, Side.OPPONENT, foe.cardId(), "BARRIER",
                EffectBlocked.COMBAT_HIT));
        assertEquals(0, attackOf(first, Side.CHALLENGER).damage());
        assertEquals(10, attackOf(first, Side.CHALLENGER).defenseAfter());
        assertEquals(3, attackOf(events(result, 2), Side.CHALLENGER).damage());
        assertFalse(events(result, 2).stream().anyMatch(EffectBlocked.class::isInstance));
    }

    @Test
    void absorption_whenDamagingFoePlayer_healsOwnPlayer() {
        // Given: il colpo 9 uccide il Bersaglio (DIF 5) e toglie 4 punti vita
        CardSnapshot mine = card("Assorbente", 9, 10, "ABSORPTION", 2);
        CardSnapshot foe = card("Bersaglio", 1, 5);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertContains(events(result, 1), applied(Phase.END, Side.CHALLENGER, mine, 2, Side.CHALLENGER,
                Target.PLAYER, Stat.LIFE_POINTS, 100, 102, false));
        assertEquals(96, result.opponentLifePoints());
    }

    // ------------------------------------------------------------------ fine di ogni round

    @Test
    void permanence_endOfRound_healsDefense() {
        // Given
        CardSnapshot mine = card("Permanente", 1, 10, "PERMANENCE", 2);
        CardSnapshot foe = card("Bersaglio", 3, 50);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then: 10 − 3 = 7, poi 7 → 9
        assertContains(events, applied(Phase.END, Side.CHALLENGER, mine, 2, Side.CHALLENGER, Target.CARD,
                Stat.DEFENSE, 7, 9, false));
    }

    @Test
    void permanence_neverHealsAboveStartingDefense() {
        // Given
        CardSnapshot mine = card("Permanente", 1, 10, "PERMANENCE", 2);
        CardSnapshot foe = card("Bersaglio", 1, 50);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then: 10 − 1 = 9, poi solo fino a 10
        assertContains(events, applied(Phase.END, Side.CHALLENGER, mine, 2, Side.CHALLENGER, Target.CARD,
                Stat.DEFENSE, 9, 10, false));
    }

    @Test
    void intensification_endOfRound_raisesAttackForTheNextRounds() {
        // Given
        CardSnapshot mine = card("Crescente", 1, 50, "INTENSIFICATION", 2);
        CardSnapshot foe = card("Bersaglio", 1, 50);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertContains(events(result, 1), applied(Phase.END, Side.CHALLENGER, mine, 2, Side.CHALLENGER,
                Target.CARD, Stat.ATTACK, 1, 3, false));
        assertEquals(3, attackOf(events(result, 2), Side.CHALLENGER).attack());
        assertEquals(3, afterRound(result, 1).challenger().attack());
    }

    @Test
    void miasma_endOfRound_damagesFoePlayerEveryRound() {
        // Given
        CardSnapshot mine = card("Miasma", 1, 50, "MIASMA", 2);
        CardSnapshot foe = card("Bersaglio", 1, 50);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertContains(events(result, 1), applied(Phase.END, Side.CHALLENGER, mine, 2, Side.OPPONENT,
                Target.PLAYER, Stat.LIFE_POINTS, 100, 98, false));
        assertContains(events(result, 2), applied(Phase.END, Side.CHALLENGER, mine, 2, Side.OPPONENT,
                Target.PLAYER, Stat.LIFE_POINTS, 98, 96, false));
    }

    @Test
    void lullaby_endOfRound_healsOwnPlayer() {
        // Given
        CardSnapshot mine = card("Ninna Nanna", 1, 50, "LULLABY", 1);
        CardSnapshot foe = card("Bersaglio", 1, 50);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertContains(events(result, 1), applied(Phase.END, Side.CHALLENGER, mine, 1, Side.CHALLENGER,
                Target.PLAYER, Stat.LIFE_POINTS, 100, 101, false));
    }

    @Test
    void endOfRoundEffect_deadCreature_doesNotTrigger() {
        // Given: il Miasma muore nel primo round
        CardSnapshot mine = card("Miasma", 1, 1, "MIASMA", 2);
        CardSnapshot foe = card("Forte", 9, 50);

        // When
        BattleResult result = simulate(mine, foe);

        // Then
        assertEquals(100, result.opponentLifePoints());
    }

    // ------------------------------------------------------------------ quando viene uccisa, quando uccide

    @Test
    void lastBreath_whenKilled_damagesTheKiller() {
        // Given
        CardSnapshot mine = card("Forte", 9, 10);
        CardSnapshot foe = card("Ultimo Respiro", 1, 5, "LAST_BREATH", 3);

        // When
        List<GameEvent> events = firstRound(mine, foe);

        // Then: 10 − 1 = 9, poi 9 → 6
        assertContains(events, applied(Phase.END, Side.OPPONENT, foe, 3, Side.CHALLENGER, Target.CARD,
                Stat.DEFENSE, 9, 6, false));
    }

    @Test
    void lastBreath_canKillTheKillerButPlayerDamageStays() {
        // Given: decisione 029, punto c
        CardSnapshot mine = card("Forte ma Fragile", 9, 4);
        CardSnapshot foe = card("Ultimo Respiro", 1, 5, "LAST_BREATH", 3);

        // When
        BattleResult result = simulate(mine, foe);

        // Then: muoiono tutte e due, ma i 9 − 5 = 4 punti vita restano tolti
        assertEquals(RoundOutcome.BOTH_DIED, result.log().rounds().getFirst().outcome());
        assertEquals(96, result.opponentLifePoints());
        assertEquals(100, result.challengerLifePoints());
    }

    @Test
    void pureTerror_whenKilling_damagesFoePlayerByOriginalDefense() {
        // Given
        CardSnapshot mine = card("Terrore", 9, 10, "PURE_TERROR", null);
        CardSnapshot foe = card("Bersaglio", 1, 5);

        // When
        BattleResult result = simulate(mine, foe);

        // Then: 4 punti vita dal combattimento, poi altri 5 (la difesa originale del Bersaglio)
        assertContains(events(result, 1), applied(Phase.END, Side.CHALLENGER, mine, null, Side.OPPONENT,
                Target.PLAYER, Stat.LIFE_POINTS, 96, 91, false));
    }

    @Test
    void pureTerror_whenBothDie_stillTriggers() {
        // Given: decisione 029, punto e
        CardSnapshot mine = card("Terrore Fragile", 9, 1, "PURE_TERROR", null);
        CardSnapshot foe = card("Bersaglio", 1, 5);

        // When
        BattleResult result = simulate(mine, foe);

        // Then: nessun danno del combattimento, solo Terrore puro
        assertEquals(RoundOutcome.BOTH_DIED, result.log().rounds().getFirst().outcome());
        assertEquals(95, result.opponentLifePoints());
    }

    // ------------------------------------------------------------------ regole comuni

    @Test
    void effectsAtTheSameTime_coinWinnerAppliesFirst() {
        // Given
        CardSnapshot mine = card("Flagellatore", 1, 10, "SCOURGE", 3);
        CardSnapshot foe = card("Altro Flagellatore", 1, 10, "SCOURGE", 2);
        for (Side coinWinner : Side.values()) {
            long seed = seedWhere(deck(mine), deck(foe), result -> result.coinTossWinner() == coinWinner);

            // When
            List<GameEvent> events = events(engine.simulate(deck(mine), deck(foe), RULES, seed, true), 1);

            // Then
            EffectApplied first = (EffectApplied) events.get(indexOf(events, EffectApplied.class));
            assertEquals(coinWinner, first.side());
        }
    }

    @Test
    void unknownEffect_throwsIllegalArgumentException() {
        // Given: un effetto tolto dal catalogo (decisione 028)
        CardSnapshot mine = card("Vecchia Carta", 1, 10, "EXILE", 1);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> simulate(mine, card("Bersaglio", 1, 10)));
    }

    @Test
    void effectWithoutItsValue_throwsIllegalArgumentException() {
        // Given
        CardSnapshot mine = card("Castigatore", 1, 10, "PUNISHMENT", null);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> simulate(mine, card("Bersaglio", 1, 10)));
    }

    @Test
    void effects_logOff_giveTheSameResult() {
        // Given
        CardSnapshot mine = card("Miasma", 3, 10, "MIASMA", 2);
        CardSnapshot foe = card("Specchio", 2, 10, "REFLECTION", 1);

        // When
        BattleResult withLog = engine.simulate(deck(mine), deck(foe), RULES, SEED, true);
        BattleResult withoutLog = engine.simulate(deck(mine), deck(foe), RULES, SEED, false);

        // Then
        assertEquals(withLog.winner(), withoutLog.winner());
        assertEquals(withLog.totalRounds(), withoutLog.totalRounds());
        assertEquals(withLog.challengerLifePoints(), withoutLog.challengerLifePoints());
        assertEquals(withLog.opponentLifePoints(), withoutLog.opponentLifePoints());
    }

    @Test
    void randomDecksWithEveryEffect_alwaysEndWithinTheRoundLimit() {
        // Given: 500 partite tra mazzi da 20 carte con effetti a caso (nessuno deve bloccare il motore)
        Random random = new Random(86L);
        EffectCode[] codes = EffectCode.values();
        Set<EffectCode> seen = EnumSet.noneOf(EffectCode.class);

        for (long seed = 0; seed < 500; seed++) {
            DeckSnapshot challengerDeck = randomDeck("C" + seed, random, codes, seen);
            DeckSnapshot opponentDeck = randomDeck("O" + seed, random, codes, seen);

            // When
            BattleResult result = engine.simulate(challengerDeck, opponentDeck, RULES, seed, true);

            // Then
            assertTrue(result.totalRounds() <= RULES.roundLimit());
            assertTrue(result.challengerLifePoints() >= 0 && result.opponentLifePoints() >= 0);
            assertEquals(result.totalRounds(), result.log().rounds().size());
        }
        assertEquals(EnumSet.allOf(EffectCode.class), seen);
    }

    // ------------------------------------------------------------------ aiuti

    private BattleResult simulate(CardSnapshot mine, CardSnapshot foe) {
        return engine.simulate(deck(mine), deck(foe), RULES, SEED, true);
    }

    private List<GameEvent> firstRound(CardSnapshot mine, CardSnapshot foe) {
        return events(simulate(mine, foe), 1);
    }

    private static List<GameEvent> events(BattleResult result, int round) {
        return result.log().rounds().get(round - 1).events();
    }

    private static AfterRound afterRound(BattleResult result, int round) {
        return result.log().rounds().get(round - 1).afterRound();
    }

    private static UUID firstPlayed(BattleResult result, Side side) {
        return events(result, 1).stream()
                .filter(CardPlayed.class::isInstance)
                .map(CardPlayed.class::cast)
                .filter(played -> played.side() == side)
                .findFirst().orElseThrow().cardId();
    }

    private static EffectApplied applied(Phase phase, Side side, CardSnapshot source, Integer value,
                                         Side targetSide, Target target, Stat stat, int before, int after,
                                         boolean untilEndOfRound) {
        return new EffectApplied(phase, side, source.cardId(), source.effects().getFirst().code(), value,
                targetSide, target, stat, before, after, untilEndOfRound);
    }

    private static void assertContains(List<GameEvent> events, GameEvent expected) {
        assertTrue(events.contains(expected), () -> "Manca " + expected + " in " + events);
    }

    private static Attack attackOf(List<GameEvent> events, Side side) {
        return events.stream()
                .filter(Attack.class::isInstance)
                .map(Attack.class::cast)
                .filter(attack -> attack.side() == side)
                .findFirst().orElseThrow();
    }

    private static int indexOf(List<GameEvent> events, Class<? extends GameEvent> type) {
        for (int i = 0; i < events.size(); i++) {
            if (type.isInstance(events.get(i))) {
                return i;
            }
        }
        throw new AssertionError("Nessun evento " + type.getSimpleName() + " in " + events);
    }

    private static int indexOfLast(List<GameEvent> events, Class<? extends GameEvent> type) {
        for (int i = events.size() - 1; i >= 0; i--) {
            if (type.isInstance(events.get(i))) {
                return i;
            }
        }
        throw new AssertionError("Nessun evento " + type.getSimpleName() + " in " + events);
    }

    private long seedWhere(DeckSnapshot challengerDeck, DeckSnapshot opponentDeck, Predicate<BattleResult> wanted) {
        for (long seed = 0; seed < 100; seed++) {
            if (wanted.test(engine.simulate(challengerDeck, opponentDeck, RULES, seed, true))) {
                return seed;
            }
        }
        throw new AssertionError("Nessun seed tra 0 e 99 dà la partita cercata");
    }

    private static CardSnapshot card(String name, int attack, int defense) {
        return card(name, attack, defense, Faction.DREAMS, null, null);
    }

    private static CardSnapshot card(String name, int attack, int defense, String effect, Integer value) {
        return card(name, attack, defense, Faction.DREAMS, effect, value);
    }

    private static CardSnapshot card(String name, int attack, int defense, Faction faction, String effect,
                                     Integer value) {
        List<CardEffect> effects = effect == null ? List.of() : List.of(new CardEffect(effect, value));
        return new CardSnapshot(id(name), name, attack, defense, faction, 1, effects);
    }

    // 20 carte diverse: una su quattro senza effetto, le altre con un effetto e un valore X a caso
    private static DeckSnapshot randomDeck(String prefix, Random random, EffectCode[] codes, Set<EffectCode> seen) {
        List<CardSnapshot> cards = new ArrayList<>();
        Faction[] factions = Faction.values();
        for (int i = 0; i < 20; i++) {
            String effect = null;
            Integer value = null;
            if (random.nextInt(4) > 0) {
                EffectCode code = codes[random.nextInt(codes.length)];
                seen.add(code);
                effect = code.name();
                value = code.hasValue() ? Integer.valueOf(1 + random.nextInt(3)) : null;
            }
            cards.add(card(prefix + "-" + i, random.nextInt(8), 1 + random.nextInt(9),
                    factions[random.nextInt(factions.length)], effect, value));
        }
        return new DeckSnapshot(cards);
    }

    private static UUID id(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    private static DeckSnapshot deck(CardSnapshot... cards) {
        return new DeckSnapshot(List.of(cards));
    }
}
