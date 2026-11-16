package it.somnia.dreamscape.engine.battle;

import it.somnia.dreamscape.engine.input.CardSnapshot;
import it.somnia.dreamscape.engine.input.EffectCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Una creatura in campo, con l'attacco e la difesa attuali e lo stato del suo effetto.
 *
 * <p>La difesa scende con i colpi e resta così nei round successivi dello stesso scontro.
 * L'attacco ha una parte permanente (cambia con Intensificazione) e le modifiche che valgono
 * solo per il round in corso (Vendetta, Indebolimento). Cosa fanno gli effetti lo decide
 * {@link BattleEngine}: qui c'è solo lo stato che cambiano.
 */
final class Creature {

    private final CardSnapshot card;
    private final EffectCode effect;
    private final int effectValue;
    private final List<AttackModifier> attackModifiers = new ArrayList<>();
    private int attack;
    private int defense;
    private int hopeShield;
    private boolean barrierUsed;
    private boolean inField = true;

    /**
     * Mette in campo una carta con le sue statistiche e il suo effetto.
     *
     * @param card la carta giocata, non {@code null}
     * @throws IllegalArgumentException se l'effetto della carta non è nel catalogo del motore
     */
    Creature(CardSnapshot card) {
        this.card = Objects.requireNonNull(card, "card");
        this.attack = card.attack();
        this.defense = card.defense();
        if (card.effects().isEmpty()) {
            this.effect = null;
            this.effectValue = 0;
        } else {
            this.effect = EffectCode.of(card.effects().getFirst());
            Integer value = card.effects().getFirst().value();
            this.effectValue = value == null ? 0 : value;
        }
    }

    CardSnapshot card() {
        return card;
    }

    /**
     * Restituisce l'effetto della carta.
     *
     * @return l'effetto, {@code null} se la carta non ne ha
     */
    EffectCode effect() {
        return effect;
    }

    /**
     * Restituisce il valore X dell'effetto.
     *
     * @return il valore X, 0 se l'effetto non ha valore o la carta non ha effetti
     */
    int effectValue() {
        return effectValue;
    }

    /**
     * Restituisce il valore X per il log.
     *
     * @return il valore X, {@code null} se l'effetto non ha valore
     */
    Integer effectValueOrNull() {
        return effect != null && effect.hasValue() ? Integer.valueOf(effectValue) : null;
    }

    /**
     * Restituisce l'attacco con le modifiche permanenti, senza bonus di fazione.
     *
     * @return l'attacco, almeno 0
     */
    int attack() {
        return attack;
    }

    /**
     * Aumenta l'attacco in modo permanente (Intensificazione).
     *
     * @param amount l'attacco in più, almeno 0
     */
    void increaseAttack(int amount) {
        attack += amount;
    }

    /**
     * Restituisce la somma delle modifiche all'attacco che valgono solo per questo round.
     *
     * @return la somma, negativa se prevale Indebolimento
     */
    int attackModifier() {
        return attackModifiers.stream().mapToInt(AttackModifier::amount).sum();
    }

    /**
     * Restituisce l'attacco di questo round senza bonus di fazione: quello permanente più le modifiche.
     *
     * @return l'attacco, almeno 0
     */
    int roundAttack() {
        return Math.max(0, attack + attackModifier());
    }

    /**
     * Aggiunge una modifica all'attacco che vale solo per questo round.
     *
     * @param code   l'effetto che la causa
     * @param amount quanto cambia l'attacco, negativo per toglierne
     */
    void addAttackModifier(EffectCode code, int amount) {
        attackModifiers.add(new AttackModifier(code, amount));
    }

    /**
     * Toglie le modifiche all'attacco di questo round e le restituisce, nell'ordine in cui sono arrivate.
     *
     * @return le modifiche tolte
     */
    List<AttackModifier> clearAttackModifiers() {
        List<AttackModifier> expired = List.copyOf(attackModifiers);
        attackModifiers.clear();
        return expired;
    }

    int defense() {
        return defense;
    }

    /**
     * Toglie difesa alla creatura. La difesa può scendere sotto 0.
     *
     * <p>La difesa in più di Speranza si consuma per prima (decisione 029, punto d).
     *
     * @param damage il danno, almeno 0
     */
    void takeDamage(int damage) {
        defense -= damage;
        hopeShield = Math.max(0, hopeShield - damage);
    }

    /**
     * Aggiunge la difesa di Speranza, che vale solo per questo round.
     *
     * @param amount la difesa in più, almeno 0
     */
    void addHopeShield(int amount) {
        defense += amount;
        hopeShield += amount;
    }

    /**
     * Toglie quello che resta della difesa di Speranza a fine round.
     *
     * @return quanta difesa è stata tolta, 0 se i colpi l'avevano già consumata
     */
    int expireHopeShield() {
        int expired = hopeShield;
        defense -= expired;
        hopeShield = 0;
        return expired;
    }

    /**
     * Recupera difesa senza superare quella iniziale della carta (Permanenza).
     *
     * @param amount la difesa da recuperare, almeno 0
     */
    void heal(int amount) {
        if (defense < card.defense()) {
            defense = Math.min(card.defense(), defense + amount);
        }
    }

    boolean isBarrierUsed() {
        return barrierUsed;
    }

    void useBarrier() {
        barrierUsed = true;
    }

    /**
     * Dice se la creatura è ancora in campo: gli effetti "mentre è in campo" valgono solo così.
     *
     * @return {@code false} dopo che è finita tra gli scarti
     */
    boolean isInField() {
        return inField;
    }

    void leaveField() {
        inField = false;
    }

    /**
     * Dice se la creatura deve morire nella fase finale.
     *
     * @return {@code true} se la difesa è 0 o meno
     */
    boolean isDead() {
        return defense <= 0;
    }

    /**
     * Una modifica all'attacco che vale solo per un round.
     *
     * @param code   l'effetto che la causa (Vendetta o Indebolimento)
     * @param amount quanto cambia l'attacco
     */
    record AttackModifier(EffectCode code, int amount) {
    }
}
