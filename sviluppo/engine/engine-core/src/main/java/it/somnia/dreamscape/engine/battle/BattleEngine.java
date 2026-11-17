package it.somnia.dreamscape.engine.battle;

import it.somnia.dreamscape.engine.BattleResult;
import it.somnia.dreamscape.engine.EndReason;
import it.somnia.dreamscape.engine.SimulationRequest;
import it.somnia.dreamscape.engine.Side;
import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.DeckSnapshot;
import it.somnia.dreamscape.engine.input.EffectCode;
import it.somnia.dreamscape.engine.input.GameRules;
import it.somnia.dreamscape.engine.log.AfterRound;
import it.somnia.dreamscape.engine.log.Attack;
import it.somnia.dreamscape.engine.log.BonusExpired;
import it.somnia.dreamscape.engine.log.CardDied;
import it.somnia.dreamscape.engine.log.CardPlayed;
import it.somnia.dreamscape.engine.log.EffectApplied;
import it.somnia.dreamscape.engine.log.EffectBlocked;
import it.somnia.dreamscape.engine.log.GameOver;
import it.somnia.dreamscape.engine.log.Phase;
import it.somnia.dreamscape.engine.log.PlayerDamage;
import it.somnia.dreamscape.engine.log.RoundOutcome;
import it.somnia.dreamscape.engine.log.Stat;
import it.somnia.dreamscape.engine.log.Target;
import it.somnia.dreamscape.engine.setup.MatchSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simula una partita completa tra due mazzi e ne restituisce il risultato e il log round per round.
 *
 * <p>Ogni round ha tre fasi (decisione 029), con gli effetti delle carte del catalogo della decisione 028:
 * <ol>
 *     <li><b>iniziale</b>: chi non ha una creatura in campo gioca la prima carta del mazzo; poi scattano
 *         gli effetti "quando viene giocata" (Castigo, Flagello, Riposo, Speranza, Vendetta);</li>
 *     <li><b>combattimento</b>: nel primo round di uno scontro agisce Indebolimento; poi le due creature
 *         si colpiscono nello stesso momento con attacco, bonus di fazione (fermato da Neutralità) e
 *         modifiche del round; Barriera annulla il primo colpo; dopo i colpi agisce Riflesso;</li>
 *     <li><b>finale</b>: muoiono le creature con difesa 0 o meno; se ne muore una sola, il suo giocatore
 *         subisce il danno del combattimento (fermato da Culla, seguito da Assorbimento); scattano
 *         "quando uccide" e "quando viene uccisa" (Terrore puro, Ultimo respiro), poi gli effetti di
 *         fine round (Permanenza, Intensificazione, Miasma, Ninna nanna); finiscono i bonus di un solo
 *         round; si controlla la fine della partita.</li>
 * </ol>
 *
 * <p>Regole comuni degli effetti:
 * <ul>
 *     <li>quando due effetti scattano nello stesso momento, li applica per primo chi ha vinto la moneta;</li>
 *     <li>Non Sequitur annulla gli effetti dell'avversaria finché è in campo, da quel momento in poi:
 *         quello che è già successo resta. Non funziona contro Immunità, e due Non Sequitur si annullano;</li>
 *     <li>Immunità ferma gli effetti dell'avversaria che agiscono sulla creatura (Castigo, Indebolimento,
 *         Riflesso, Ultimo respiro, Non Sequitur), non quelli sui punti vita del giocatore;</li>
 *     <li>Culla protegge il suo giocatore fino alla fine del round in cui la creatura muore;</li>
 *     <li>un effetto si scrive nel log solo se cambia un valore.</li>
 * </ul>
 *
 * <p>Il motore è deterministico: con gli stessi mazzi, le stesse regole, lo stesso seed e la stessa
 * {@link #ENGINE_VERSION} la partita è sempre identica. La classe non ha stato e si può riusare.
 */
public final class BattleEngine {

    /** La versione delle regole del motore: va cambiata a ogni modifica che cambia l'esito delle partite. */
    public static final String ENGINE_VERSION = "0.2.0";

    /** Le carte che servono nella pila degli scarti perché Vendetta scatti (decisione 028). */
    static final int VENGEANCE_MIN_DISCARD = 5;

    /** La difesa sotto cui Castigo non fa scendere la creatura avversaria: Castigo non uccide. */
    static final int PUNISHMENT_MIN_DEFENSE = 1;

    /**
     * Simula la partita descritta da una richiesta di engine-service.
     *
     * @param request mazzi, regole, seed e se serve il log, non {@code null}
     * @return il risultato della partita, con il log se richiesto
     * @throws NullPointerException     se la richiesta è {@code null}
     * @throws IllegalArgumentException se una carta giocata ha un effetto che il motore non conosce
     */
    public BattleResult simulate(SimulationRequest request) {
        Objects.requireNonNull(request, "request");
        return simulate(request.challengerDeck(), request.opponentDeck(), request.rules(), request.seed(),
                request.withLog());
    }

    /**
     * Simula una partita.
     *
     * @param challengerDeck il mazzo di chi ha avviato la partita, non {@code null}
     * @param opponentDeck   il mazzo dell'avversario, non {@code null}
     * @param rules          punti vita, limite di round e vantaggi di fazione, non {@code null}
     * @param seed           il seed della partita, salvato dal backend in {@code matches.random_seed}
     * @param withLog        {@code false} per non creare il log (simulazioni, #87)
     * @return il risultato della partita, con il log se richiesto
     * @throws NullPointerException     se un mazzo o le regole sono {@code null}
     * @throws IllegalArgumentException se una carta giocata ha un effetto che il motore non conosce
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
                boolean newClash = playStartPhase();
                CombatHits hits = playCombatPhase(newClash);
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

        // ------------------------------------------------------------------ fase iniziale

        /**
         * Fase iniziale: chi non ha una creatura in campo gioca la prima carta del mazzo, poi scattano
         * gli effetti "quando viene giocata" delle carte appena entrate.
         *
         * <p>Nel log gioca prima {@code CHALLENGER}, come nell'esempio di schema.md; gli effetti invece
         * seguono la moneta e scattano quando tutte e due le creature sono in campo, così un Non Sequitur
         * appena giocato annulla già l'effetto dell'avversaria. Una carta nuova apre un nuovo scontro.
         * Il mazzo non è mai vuoto qui: il mazzo esaurito chiude la partita nella fase finale del round prima.
         *
         * @return {@code true} se in questo round è iniziato un nuovo scontro
         */
        private boolean playStartPhase() {
            List<PlayerSide> played = new ArrayList<>();
            for (PlayerSide side : List.of(challenger, opponent)) {
                if (!side.hasCreature()) {
                    Creature creature = side.playTopCard();
                    recorder.record(() -> new CardPlayed(side.side(), creature.card().cardId(), creature.attack(),
                            creature.defense()));
                    played.add(side);
                }
            }
            if (played.isEmpty()) {
                return false;
            }
            clash++;
            for (Fighter own : fightersInCoinOrder()) {
                Fighter foe = foeOf(own);
                // Si dice una volta, all'inizio dello scontro: il Non Sequitur avversario qui non funziona
                if (own.has(EffectCode.IMMUNITY) && foe.has(EffectCode.NON_SEQUITUR)) {
                    recordBlocked(Phase.START, own, EffectCode.IMMUNITY, EffectCode.NON_SEQUITUR.name());
                }
            }
            for (Fighter own : fightersInCoinOrder()) {
                if (played.contains(own.side())) {
                    applyOnPlayEffect(own, foeOf(own));
                }
            }
            return true;
        }

        private void applyOnPlayEffect(Fighter own, Fighter foe) {
            int value = own.creature().effectValue();
            EffectCode effect = own.creature().effect();
            if (effect == null || (effect.hasValue() && value == 0)) {
                return;
            }
            switch (effect) {
                case PUNISHMENT -> {
                    // Castigo non uccide: la difesa dell'avversaria resta almeno a 1
                    int damage = Math.min(value, foe.creature().defense() - PUNISHMENT_MIN_DEFENSE);
                    if (damage > 0 && triggers(own, foe, effect, Phase.START)
                            && !isImmune(foe, effect, Phase.START)) {
                        damageDefense(own, foe, damage, Phase.START);
                    }
                }
                case SCOURGE -> {
                    if (triggers(own, foe, effect, Phase.START)
                            && !cradleProtects(foe, own, effect.name(), Phase.START)) {
                        changeLifePoints(own, foe.side(), -value, Phase.START);
                    }
                }
                case REST -> {
                    if (triggers(own, foe, effect, Phase.START)) {
                        changeLifePoints(own, own.side(), value, Phase.START);
                    }
                }
                case HOPE -> {
                    if (triggers(own, foe, effect, Phase.START)) {
                        int before = own.creature().defense();
                        own.creature().addHopeShield(value);
                        recordApplied(Phase.START, own, own.side(), Target.CARD, Stat.DEFENSE, before,
                                own.creature().defense(), true);
                    }
                }
                case VENGEANCE -> {
                    if (own.side().cardsInDiscard() >= VENGEANCE_MIN_DISCARD
                            && triggers(own, foe, effect, Phase.START)) {
                        int before = own.creature().roundAttack();
                        own.creature().addAttackModifier(effect, value);
                        recordApplied(Phase.START, own, own.side(), Target.CARD, Stat.ATTACK, before,
                                own.creature().roundAttack(), true);
                    }
                }
                default -> {
                    // Gli altri effetti non scattano quando la carta viene giocata
                }
            }
        }

        // ------------------------------------------------------------------ combattimento

        /**
         * Combattimento: le due creature si colpiscono nello stesso momento.
         *
         * <p>Entrambi i colpi si calcolano sulle creature com'erano prima del combattimento, poi si
         * applicano insieme. Nel log viene prima il colpo di chi ha vinto la moneta. Riflesso agisce
         * dopo i due colpi e può uccidere (decisione 029, punto f).
         *
         * @param newClash {@code true} nel primo round di uno scontro, l'unico in cui agisce Indebolimento
         * @return i due colpi, che servono alla fase finale per il danno al giocatore
         */
        private CombatHits playCombatPhase(boolean newClash) {
            List<Fighter> fighters = fightersInCoinOrder();
            if (newClash) {
                for (Fighter own : fighters) {
                    applyWeakening(own, foeOf(own));
                }
            }

            List<Hit> hits = new ArrayList<>();
            for (Fighter attacker : fighters) {
                hits.add(computeHit(attacker, foeOf(attacker)));
            }
            for (Hit hit : hits) {
                foeOf(hit.attacker()).creature().takeDamage(hit.damage());
            }

            for (Fighter own : fighters) {
                Fighter foe = foeOf(own);
                int damageTaken = hitOf(hits, foe).damage();
                int value = own.creature().effectValue();
                if (damageTaken > 0 && value > 0 && triggers(own, foe, EffectCode.REFLECTION, Phase.COMBAT)
                        && !isImmune(foe, EffectCode.REFLECTION, Phase.COMBAT)) {
                    damageDefense(own, foe, value, Phase.COMBAT);
                }
            }
            return new CombatHits(hitOf(hits, fighter(challenger)), hitOf(hits, fighter(opponent)));
        }

        private void applyWeakening(Fighter own, Fighter foe) {
            int value = own.creature().effectValue();
            if (value == 0 || !triggers(own, foe, EffectCode.WEAKENING, Phase.COMBAT)
                    || isImmune(foe, EffectCode.WEAKENING, Phase.COMBAT)) {
                return;
            }
            int before = foe.creature().roundAttack();
            foe.creature().addAttackModifier(EffectCode.WEAKENING, -value);
            recordApplied(Phase.COMBAT, own, foe.side(), Target.CARD, Stat.ATTACK, before,
                    foe.creature().roundAttack(), true);
        }

        /**
         * Calcola un colpo e lo scrive nel log, senza ancora applicarlo.
         *
         * <p>Il danno è attacco più bonus di fazione più modifiche del round, mai sotto 0 (decisione 029,
         * punto b). Neutralità di chi è colpito toglie il bonus di fazione; Barriera annulla il primo
         * colpo che farebbe danno.
         */
        private Hit computeHit(Fighter attacker, Fighter defender) {
            Creature attackerCard = attacker.creature();
            Creature defenderCard = defender.creature();

            int factionBonus = rules.attackBonus(attackerCard.card().faction(), defenderCard.card().faction());
            if (factionBonus != 0 && triggers(defender, attacker, EffectCode.NEUTRALITY, Phase.COMBAT)) {
                recordBlocked(Phase.COMBAT, defender, EffectCode.NEUTRALITY, EffectBlocked.FACTION_BONUS);
                factionBonus = 0;
            }

            int attackModifier = attackerCard.attackModifier();
            int damage = Math.max(0, attackerCard.attack() + factionBonus + attackModifier);
            if (damage > 0 && !defenderCard.isBarrierUsed()
                    && triggers(defender, attacker, EffectCode.BARRIER, Phase.COMBAT)) {
                defenderCard.useBarrier();
                recordBlocked(Phase.COMBAT, defender, EffectCode.BARRIER, EffectBlocked.COMBAT_HIT);
                damage = 0;
            }

            int defenseBefore = defenderCard.defense();
            int finalBonus = factionBonus;
            int finalDamage = damage;
            recorder.record(() -> new Attack(attacker.side().side(), attackerCard.card().cardId(),
                    attackerCard.attack(), finalBonus, attackModifier, finalDamage, defenseBefore,
                    defenseBefore - finalDamage));
            return new Hit(attacker, damage, defenseBefore);
        }

        // ------------------------------------------------------------------ fase finale

        /**
         * Fase finale, nell'ordine della decisione 029 (punto c): morti, danno del combattimento al
         * giocatore, "quando uccide" e "quando viene uccisa", effetti di fine round, fine dei bonus.
         *
         * <p>Con una sola creatura morta, il suo giocatore subisce l'attacco totale della vincitrice meno
         * la difesa che la sconfitta aveva prima del colpo, mai sotto 0 (punto a). Con due creature
         * morte nessun danno. Ultimo respiro può uccidere la creatura che ha ucciso: il danno al
         * giocatore già inflitto resta.
         *
         * @param hits i due colpi del combattimento
         * @return chi è morto nel round, contando anche le morti causate dagli effetti
         */
        private RoundOutcome playEndPhase(CombatHits hits) {
            // Le creature si prendono prima delle morti: gli effetti di chi muore scattano ancora
            Fighter challengerFighter = fighter(challenger);
            Fighter opponentFighter = fighter(opponent);
            boolean challengerDied = challengerFighter.creature().isDead();
            boolean opponentDied = opponentFighter.creature().isDead();
            if (challengerDied) {
                die(challengerFighter);
            }
            if (opponentDied) {
                die(opponentFighter);
            }

            if (challengerDied && !opponentDied) {
                damagePlayer(challengerFighter, opponentFighter, hits.opponentHit());
            } else if (opponentDied && !challengerDied) {
                damagePlayer(opponentFighter, challengerFighter, hits.challengerHit());
            }

            List<Fighter> fighters = coinTossWinner == Side.CHALLENGER
                    ? List.of(challengerFighter, opponentFighter)
                    : List.of(opponentFighter, challengerFighter);
            for (Fighter own : fighters) {
                Fighter foe = own == challengerFighter ? opponentFighter : challengerFighter;
                boolean ownDied = own == challengerFighter ? challengerDied : opponentDied;
                boolean foeDied = own == challengerFighter ? opponentDied : challengerDied;
                applyDeathAndKillEffects(own, foe, ownDied, foeDied);
            }
            for (Fighter own : fighters) {
                if (own.creature().isInField()) {
                    Fighter foe = own == challengerFighter ? opponentFighter : challengerFighter;
                    applyEndOfRoundEffect(own, foe);
                }
            }
            for (Fighter own : fighters) {
                if (own.creature().isInField()) {
                    expireRoundBonuses(own);
                }
            }

            boolean challengerGone = !challengerFighter.creature().isInField();
            boolean opponentGone = !opponentFighter.creature().isInField();
            if (challengerGone && opponentGone) {
                return RoundOutcome.BOTH_DIED;
            }
            if (challengerGone) {
                return RoundOutcome.CHALLENGER_CARD_DIED;
            }
            return opponentGone ? RoundOutcome.OPPONENT_CARD_DIED : RoundOutcome.NONE_DIED;
        }

        private void die(Fighter fighter) {
            fighter.side().discardCreature();
            recorder.record(() -> new CardDied(fighter.side().side(), fighter.creature().card().cardId()));
        }

        /**
         * Applica il danno del combattimento al giocatore della creatura sconfitta.
         *
         * <p>L'evento {@code PLAYER_DAMAGE} c'è anche quando il danno è 0. Culla della creatura sconfitta
         * ferma il danno anche nel round in cui muore. Se i punti vita scendono davvero, scatta
         * Assorbimento della vincitrice.
         */
        private void damagePlayer(Fighter loser, Fighter winner, Hit winningHit) {
            int amount = Math.max(0, winningHit.damage() - winningHit.targetDefenseBefore());
            if (amount > 0 && cradleProtects(loser, winner, EffectBlocked.PLAYER_DAMAGE, Phase.END)) {
                return;
            }
            int lifePointsBefore = loser.side().lifePoints();
            loser.side().loseLifePoints(amount);
            int lifePointsAfter = loser.side().lifePoints();
            recorder.record(() -> new PlayerDamage(loser.side().side(), amount, lifePointsBefore, lifePointsAfter));

            int value = winner.creature().effectValue();
            if (lifePointsAfter < lifePointsBefore && value > 0
                    && triggers(winner, loser, EffectCode.ABSORPTION, Phase.END)) {
                changeLifePoints(winner, winner.side(), value, Phase.END);
            }
        }

        /**
         * Applica "quando viene uccisa" (Ultimo respiro) e "quando uccide" (Terrore puro) di una creatura.
         *
         * <p>Ogni creatura morta è stata uccisa dalla creatura avversaria, anche se è morta per Riflesso.
         * Terrore puro scatta anche quando muoiono tutte e due (decisione 029, punto e); Ultimo respiro
         * non fa niente se anche chi ha ucciso è già morta.
         */
        private void applyDeathAndKillEffects(Fighter own, Fighter foe, boolean ownDied, boolean foeDied) {
            int value = own.creature().effectValue();
            if (ownDied && value > 0 && foe.creature().isInField()
                    && triggers(own, foe, EffectCode.LAST_BREATH, Phase.END)
                    && !isImmune(foe, EffectCode.LAST_BREATH, Phase.END)) {
                damageDefense(own, foe, value, Phase.END);
                if (foe.creature().isDead()) {
                    die(foe);
                }
            }
            if (foeDied && triggers(own, foe, EffectCode.PURE_TERROR, Phase.END)
                    && !cradleProtects(foe, own, EffectCode.PURE_TERROR.name(), Phase.END)) {
                changeLifePoints(own, foe.side(), -foe.creature().card().defense(), Phase.END);
            }
        }

        private void applyEndOfRoundEffect(Fighter own, Fighter foe) {
            Creature creature = own.creature();
            int value = creature.effectValue();
            EffectCode effect = creature.effect();
            if (effect == null || value == 0) {
                return;
            }
            switch (effect) {
                case PERMANENCE -> {
                    if (creature.defense() < creature.card().defense() && triggers(own, foe, effect, Phase.END)) {
                        int before = creature.defense();
                        creature.heal(value);
                        recordApplied(Phase.END, own, own.side(), Target.CARD, Stat.DEFENSE, before,
                                creature.defense(), false);
                    }
                }
                case INTENSIFICATION -> {
                    if (triggers(own, foe, effect, Phase.END)) {
                        int before = creature.attack();
                        creature.increaseAttack(value);
                        recordApplied(Phase.END, own, own.side(), Target.CARD, Stat.ATTACK, before,
                                creature.attack(), false);
                    }
                }
                case MIASMA -> {
                    if (triggers(own, foe, effect, Phase.END)
                            && !cradleProtects(foe, own, effect.name(), Phase.END)) {
                        changeLifePoints(own, foe.side(), -value, Phase.END);
                    }
                }
                case LULLABY -> {
                    if (triggers(own, foe, effect, Phase.END)) {
                        changeLifePoints(own, own.side(), value, Phase.END);
                    }
                }
                default -> {
                    // Gli altri effetti non scattano a fine round
                }
            }
        }

        /**
         * Fa finire i bonus che valgono un solo round su una creatura ancora in campo: quello che resta
         * della difesa di Speranza (decisione 029, punto d) e le modifiche all'attacco di Vendetta e
         * Indebolimento. Scrive {@code BONUS_EXPIRED} solo se il valore cambia.
         */
        private void expireRoundBonuses(Fighter own) {
            Creature creature = own.creature();
            int defenseBefore = creature.defense();
            if (creature.expireHopeShield() > 0) {
                int defenseAfter = creature.defense();
                recorder.record(() -> new BonusExpired(own.side().side(), creature.card().cardId(),
                        EffectCode.HOPE.name(), Stat.DEFENSE, defenseBefore, defenseAfter));
            }

            int remaining = creature.attackModifier();
            for (Creature.AttackModifier modifier : creature.clearAttackModifiers()) {
                int before = Math.max(0, creature.attack() + remaining);
                remaining -= modifier.amount();
                int after = Math.max(0, creature.attack() + remaining);
                if (before != after) {
                    recorder.record(() -> new BonusExpired(own.side().side(), creature.card().cardId(),
                            modifier.code().name(), Stat.ATTACK, before, after));
                }
            }
        }

        // ------------------------------------------------------------------ regole comuni degli effetti

        /**
         * Dice se l'effetto di una creatura scatta adesso.
         *
         * <p>Non scatta se la creatura non ha quell'effetto, oppure se lo annulla il Non Sequitur
         * dell'avversaria: in questo caso lo scrive nel log.
         *
         * @param own    la creatura con l'effetto
         * @param foe    la creatura avversaria
         * @param effect l'effetto che dovrebbe scattare
         * @param phase  la fase, per il log
         * @return {@code true} se l'effetto si applica
         */
        private boolean triggers(Fighter own, Fighter foe, EffectCode effect, Phase phase) {
            if (!own.has(effect)) {
                return false;
            }
            if (isAnnulledByNonSequitur(own.creature(), foe.creature())) {
                recordBlocked(phase, foe, EffectCode.NON_SEQUITUR, effect.name());
                return false;
            }
            return true;
        }

        /**
         * Dice se il Non Sequitur di {@code foe} annulla l'effetto di {@code own}.
         *
         * <p>Vale solo finché la creatura con Non Sequitur è in campo: quando muore, gli effetti
         * dell'avversaria tornano. Non funziona contro Immunità, e due Non Sequitur in campo si annullano.
         */
        private static boolean isAnnulledByNonSequitur(Creature own, Creature foe) {
            if (foe.effect() != EffectCode.NON_SEQUITUR || !foe.isInField()) {
                return false;
            }
            if (own.effect() == EffectCode.IMMUNITY) {
                return false;
            }
            return !(own.effect() == EffectCode.NON_SEQUITUR && own.isInField());
        }

        /**
         * Dice se Immunità protegge la creatura da un effetto dell'avversaria, e lo scrive nel log.
         * Immunità non si può annullare: Non Sequitur non funziona contro di lei.
         */
        private boolean isImmune(Fighter target, EffectCode blocked, Phase phase) {
            if (!target.has(EffectCode.IMMUNITY)) {
                return false;
            }
            recordBlocked(phase, target, EffectCode.IMMUNITY, blocked.name());
            return true;
        }

        /**
         * Dice se Culla protegge il giocatore da un danno ai punti vita, e lo scrive nel log.
         *
         * <p>{@code protectedFighter} è la creatura che era in campo all'inizio della fase: Culla vale
         * fino alla fine del round in cui muore.
         */
        private boolean cradleProtects(Fighter protectedFighter, Fighter foe, String blocked, Phase phase) {
            if (!triggers(protectedFighter, foe, EffectCode.CRADLE, phase)) {
                return false;
            }
            recordBlocked(phase, protectedFighter, EffectCode.CRADLE, blocked);
            return true;
        }

        // Danno alla difesa causato dall'effetto di source: la difesa può scendere sotto 0
        private void damageDefense(Fighter source, Fighter target, int amount, Phase phase) {
            int before = target.creature().defense();
            target.creature().takeDamage(amount);
            recordApplied(phase, source, target.side(), Target.CARD, Stat.DEFENSE, before,
                    target.creature().defense(), false);
        }

        // Punti vita tolti (delta negativo, mai sotto 0) o aggiunti dall'effetto di source
        private void changeLifePoints(Fighter source, PlayerSide target, int delta, Phase phase) {
            int before = target.lifePoints();
            if (delta < 0) {
                target.loseLifePoints(-delta);
            } else {
                target.gainLifePoints(delta);
            }
            recordApplied(phase, source, target, Target.PLAYER, Stat.LIFE_POINTS, before, target.lifePoints(),
                    false);
        }

        private void recordApplied(Phase phase, Fighter source, PlayerSide targetSide, Target target, Stat stat,
                                   int before, int after, boolean untilEndOfRound) {
            if (before == after) {
                return;
            }
            Creature creature = source.creature();
            recorder.record(() -> new EffectApplied(phase, source.side().side(), creature.card().cardId(),
                    creature.effect().name(), creature.effectValueOrNull(), targetSide.side(), target, stat,
                    before, after, untilEndOfRound));
        }

        private void recordBlocked(Phase phase, Fighter blocker, EffectCode blockerEffect, String blocked) {
            recorder.record(() -> new EffectBlocked(phase, blocker.side().side(),
                    blocker.creature().card().cardId(), blockerEffect.name(), blocked));
        }

        // ------------------------------------------------------------------ creature in campo

        private Fighter fighter(PlayerSide side) {
            return new Fighter(side, side.creature());
        }

        private Fighter foeOf(Fighter fighter) {
            return fighter(fighter.side() == challenger ? opponent : challenger);
        }

        // Le due creature in campo: per prima quella di chi ha vinto la moneta
        private List<Fighter> fightersInCoinOrder() {
            return coinTossWinner == Side.CHALLENGER
                    ? List.of(fighter(challenger), fighter(opponent))
                    : List.of(fighter(opponent), fighter(challenger));
        }

        private static Hit hitOf(List<Hit> hits, Fighter attacker) {
            return hits.stream().filter(hit -> hit.attacker().side() == attacker.side()).findFirst().orElseThrow();
        }

        // ------------------------------------------------------------------ fine della partita

        /**
         * Controlla se la partita è finita, nell'ordine della decisione 029: punti vita, mazzo esaurito,
         * limite di round.
         *
         * <p>Se le due condizioni valgono per tutti e due i lati nello stesso round, è pareggio.
         * I punti vita si guardano solo qui: un giocatore arrivato a 0 durante il round e poi curato
         * da un effetto resta in partita.
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
     * Una creatura con il suo lato. Si tiene da parte prima delle morti, perché gli effetti di una
     * creatura morta (Ultimo respiro, Culla) scattano quando il lato non l'ha più in campo.
     *
     * @param side     il lato della creatura
     * @param creature la creatura, in campo o appena morta
     */
    private record Fighter(PlayerSide side, Creature creature) {

        boolean has(EffectCode effect) {
            return creature.effect() == effect;
        }
    }

    /**
     * Un colpo del combattimento, calcolato prima che i due colpi si applichino.
     *
     * @param attacker            la creatura che colpisce, con il suo lato
     * @param damage              il danno inflitto, almeno 0 (0 se annullato da Barriera)
     * @param targetDefenseBefore la difesa della creatura colpita prima del colpo
     */
    private record Hit(Fighter attacker, int damage, int targetDefenseBefore) {
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
