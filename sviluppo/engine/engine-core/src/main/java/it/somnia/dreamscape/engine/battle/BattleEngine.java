package it.somnia.dreamscape.engine.battle;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.GameRules;
import it.somnia.dreamscape.engine.log.AfterRound;
import it.somnia.dreamscape.engine.log.Attack;
import it.somnia.dreamscape.engine.log.CardDied;
import it.somnia.dreamscape.engine.log.CardPlayed;
import it.somnia.dreamscape.engine.log.GameOver;
import it.somnia.dreamscape.engine.log.PlayerDamage;
import it.somnia.dreamscape.engine.log.RoundOutcome;
import it.somnia.dreamscape.engine.setup.MatchSetup;

import java.util.List;
import java.util.Objects;

/**
 * Simula una partita completa tra due mazzi e ne restituisce il risultato e il log round per round.
 *
 * <p>Ogni round ha tre fasi (decisione 029):
 * <ol>
 *     <li><b>iniziale</b>: chi non ha una creatura in campo gioca la prima carta del mazzo;</li>
 *     <li><b>combattimento</b>: le due creature si colpiscono nello stesso momento con attacco più
 *         bonus di fazione; le ferite restano nei round successivi dello stesso scontro;</li>
 *     <li><b>finale</b>: muoiono le creature con difesa 0 o meno; se ne muore una sola, il suo giocatore
 *         subisce l'attacco totale della vincitrice meno la difesa che la sconfitta aveva prima del colpo;
 *         poi si controlla la fine della partita (punti vita, mazzo esaurito, limite di round).</li>
 * </ol>
 *
 * <p><b>Gli effetti delle carte per ora sono ignorati:</b> una carta con un effetto gioca solo con
 * attacco e difesa. Gli effetti arrivano con la #86, dentro le fasi di questa classe.
 *
 * <p>Il motore è deterministico: con gli stessi mazzi, le stesse regole, lo stesso seed e la stessa
 * {@link #ENGINE_VERSION} la partita è sempre identica. La classe non ha stato e si può riusare.
 */
public final class BattleEngine {

    /** La versione delle regole del motore: va cambiata a ogni modifica che cambia l'esito delle partite. */
    public static final String ENGINE_VERSION = "0.1.0";

    /**
     * Simula una partita.
     *
     * @param challengerDeck il mazzo di chi ha avviato la partita, non {@code null}
     * @param opponentDeck   il mazzo dell'avversario, non {@code null}
     * @param rules          punti vita, limite di round e vantaggi di fazione, non {@code null}
     * @param seed           il seed della partita, salvato dal backend in {@code matches.random_seed}
     * @param withLog        {@code false} per non creare il log (simulazioni, #87)
     * @return il risultato della partita, con il log se richiesto
     * @throws NullPointerException se un mazzo o le regole sono {@code null}
     */
    public BattleResult simulate(DeckSnapshot challengerDeck, DeckSnapshot opponentDeck, GameRules rules,
                                 long seed, boolean withLog) {
        Objects.requireNonNull(challengerDeck, "challengerDeck");
        Objects.requireNonNull(opponentDeck, "opponentDeck");
        Objects.requireNonNull(rules, "rules");

        MatchSetup<CardSnapshot> setup =
                MatchSetup.prepare(challengerDeck.expandCopies(), opponentDeck.expandCopies(), seed);
        return new Match(setup, rules, withLog).play();
    }

    /**
     * Una singola partita in corso: tiene lo stato che cambia da un round all'altro.
     */
    private static final class Match {

        private final GameRules rules;
        private final Side coinTossWinner;
        private final PlayerSide challenger;
        private final PlayerSide opponent;
        private final BattleRecorder recorder;
        private int round;
        private int clash;

        Match(MatchSetup<CardSnapshot> setup, GameRules rules, boolean withLog) {
            this.rules = rules;
            this.coinTossWinner = setup.coinTossWinner();
            this.challenger = new PlayerSide(Side.CHALLENGER, setup.challengerDeck(), rules.startingLifePoints());
            this.opponent = new PlayerSide(Side.OPPONENT, setup.opponentDeck(), rules.startingLifePoints());
            this.recorder = new BattleRecorder(withLog);
        }

        BattleResult play() {
            GameOver gameOver = null;
            while (gameOver == null) {
                round++;
                playStartPhase();
                CombatHits hits = playCombatPhase();
                RoundOutcome outcome = playEndPhase(hits);
                gameOver = checkGameOver();
                if (gameOver != null) {
                    GameOver finalEvent = gameOver;
                    recorder.record(() -> finalEvent);
                }
                recorder.endRound(round, clash, outcome,
                        () -> new AfterRound(challenger.toState(), opponent.toState()));
            }
            return new BattleResult(gameOver.winner(), gameOver.endReason(), round, challenger.lifePoints(),
                    opponent.lifePoints(), coinTossWinner, recorder.build(ENGINE_VERSION, coinTossWinner));
        }

        /**
         * Fase iniziale: chi non ha una creatura in campo gioca la prima carta del mazzo.
         *
         * <p>Nel log gioca prima {@code CHALLENGER}, come nell'esempio di schema.md. Una carta nuova
         * apre un nuovo scontro. Il mazzo non è mai vuoto qui: il mazzo esaurito chiude la partita
         * nella fase finale del round prima.
         */
        private void playStartPhase() {
            boolean newClash = false;
            for (PlayerSide side : List.of(challenger, opponent)) {
                if (!side.hasCreature()) {
                    Creature played = side.playTopCard();
                    recorder.record(() -> new CardPlayed(side.side(), played.card().cardId(), played.attack(),
                            played.defense()));
                    newClash = true;
                }
            }
            if (newClash) {
                clash++;
            }
        }

        /**
         * Combattimento: le due creature si colpiscono nello stesso momento.
         *
         * <p>Entrambi i colpi si calcolano sulle creature com'erano prima del combattimento, poi si
         * applicano insieme. Nel log viene prima il colpo di chi ha vinto la moneta.
         *
         * @return i due colpi, che servono alla fase finale per il danno al giocatore
         */
        private CombatHits playCombatPhase() {
            Hit challengerHit = computeHit(challenger, opponent);
            Hit opponentHit = computeHit(opponent, challenger);
            opponent.creature().takeDamage(challengerHit.damage());
            challenger.creature().takeDamage(opponentHit.damage());

            Hit first = coinTossWinner == Side.CHALLENGER ? challengerHit : opponentHit;
            Hit second = first == challengerHit ? opponentHit : challengerHit;
            for (Hit hit : List.of(first, second)) {
                PlayerSide target = hit.attacker() == challenger ? opponent : challenger;
                recorder.record(() -> new Attack(hit.attacker().side(), hit.attackerCard().card().cardId(),
                        hit.attackerCard().attack(), hit.factionBonus(), hit.attackModifier(), hit.damage(),
                        hit.targetDefenseBefore(), target.creature().defense()));
            }
            return new CombatHits(challengerHit, opponentHit);
        }

        private Hit computeHit(PlayerSide attacker, PlayerSide defender) {
            Creature attackerCard = attacker.creature();
            Creature defenderCard = defender.creature();
            int factionBonus = rules.attackBonus(attackerCard.card().faction(), defenderCard.card().faction());
            // Nessuna modifica degli effetti finché non arriva la #86
            int attackModifier = 0;
            // L'attacco non scende mai sotto 0 (decisione 029, punto b)
            int damage = Math.max(0, attackerCard.attack() + factionBonus + attackModifier);
            return new Hit(attacker, attackerCard, factionBonus, attackModifier, damage, defenderCard.defense());
        }

        /**
         * Fase finale: morte delle creature e danno al giocatore.
         *
         * <p>Con una sola creatura morta, il suo giocatore subisce l'attacco totale della vincitrice meno
         * la difesa che la sconfitta aveva prima del colpo, mai sotto 0 (decisione 029, punto a).
         * L'evento {@code PLAYER_DAMAGE} c'è anche quando il danno è 0. Con due creature morte nessun danno.
         *
         * @param hits i due colpi del combattimento
         * @return chi è morto nel round
         */
        private RoundOutcome playEndPhase(CombatHits hits) {
            boolean challengerDied = challenger.creature().isDead();
            boolean opponentDied = opponent.creature().isDead();
            if (challengerDied) {
                discard(challenger);
            }
            if (opponentDied) {
                discard(opponent);
            }

            if (challengerDied && !opponentDied) {
                damagePlayer(challenger, hits.opponentHit());
                return RoundOutcome.CHALLENGER_CARD_DIED;
            }
            if (opponentDied && !challengerDied) {
                damagePlayer(opponent, hits.challengerHit());
                return RoundOutcome.OPPONENT_CARD_DIED;
            }
            return challengerDied ? RoundOutcome.BOTH_DIED : RoundOutcome.NONE_DIED;
        }

        private void discard(PlayerSide side) {
            Creature dead = side.creature();
            side.discardCreature();
            recorder.record(() -> new CardDied(side.side(), dead.card().cardId()));
        }

        private void damagePlayer(PlayerSide loser, Hit winningHit) {
            int amount = Math.max(0, winningHit.damage() - winningHit.targetDefenseBefore());
            int lifePointsBefore = loser.lifePoints();
            loser.loseLifePoints(amount);
            recorder.record(() -> new PlayerDamage(loser.side(), amount, lifePointsBefore, loser.lifePoints()));
        }

        /**
         * Controlla se la partita è finita, nell'ordine della decisione 029: punti vita, mazzo esaurito,
         * limite di round.
         *
         * <p>Se le due condizioni valgono per tutti e due i lati nello stesso round, è pareggio.
         *
         * @return l'evento di fine partita, {@code null} se la partita continua
         */
        private GameOver checkGameOver() {
            GameOver byLifePoints = loserCheck(challenger.lifePoints() == 0, opponent.lifePoints() == 0,
                    EndReason.LIFE_POINTS);
            if (byLifePoints != null) {
                return byLifePoints;
            }
            GameOver byDeck = loserCheck(challenger.isDeckExhausted(), opponent.isDeckExhausted(),
                    EndReason.DECK_EXHAUSTED);
            if (byDeck != null) {
                return byDeck;
            }
            if (round >= rules.roundLimit()) {
                return new GameOver(null, EndReason.ROUND_LIMIT);
            }
            return null;
        }

        private static GameOver loserCheck(boolean challengerLost, boolean opponentLost, EndReason reason) {
            if (challengerLost && opponentLost) {
                return new GameOver(null, reason);
            }
            if (challengerLost) {
                return new GameOver(Side.OPPONENT, reason);
            }
            if (opponentLost) {
                return new GameOver(Side.CHALLENGER, reason);
            }
            return null;
        }
    }

    /**
     * Un colpo del combattimento, calcolato prima che i due colpi si applichino.
     *
     * @param attacker            il lato che colpisce
     * @param attackerCard        la creatura che colpisce
     * @param factionBonus        l'attacco in più per il vantaggio di fazione
     * @param attackModifier      le modifiche di questo round (per ora sempre 0)
     * @param damage              il danno inflitto, almeno 0
     * @param targetDefenseBefore la difesa della creatura colpita prima del colpo
     */
    private record Hit(PlayerSide attacker, Creature attackerCard, int factionBonus, int attackModifier, int damage,
                       int targetDefenseBefore) {
    }

    /**
     * I due colpi di un combattimento.
     *
     * @param challengerHit il colpo della creatura di {@code CHALLENGER}
     * @param opponentHit   il colpo della creatura di {@code OPPONENT}
     */
    private record CombatHits(Hit challengerHit, Hit opponentHit) {
    }
}
