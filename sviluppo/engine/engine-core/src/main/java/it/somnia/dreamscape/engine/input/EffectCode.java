package it.somnia.dreamscape.engine.input;

import java.util.Objects;

/**
 * I 19 effetti del catalogo provvisorio (decisione 028), con gli stessi codici di {@code effects.code}.
 *
 * <p>Qui c'è solo se l'effetto ha il valore X: il comportamento sta nelle fasi di
 * {@code BattleEngine}. Un effetto nuovo si aggiunge qui e nel motore, e cambia la versione del motore.
 */
public enum EffectCode {

    /** Castigo X: quando viene giocata, X danni alla difesa dell'avversaria, che resta almeno a 1. */
    PUNISHMENT(true),

    /** Flagello X: quando viene giocata, X danni ai punti vita dell'avversario. */
    SCOURGE(true),

    /** Riposo X: quando viene giocata, il suo giocatore guadagna X punti vita. */
    REST(true),

    /** Speranza X: quando viene giocata, +X difesa nel primo round dello scontro. */
    HOPE(true),

    /** Vendetta X: quando viene giocata, +X attacco nel primo round, con almeno 5 carte negli scarti. */
    VENGEANCE(true),

    /** Indebolimento X: l'avversaria ha X attacco in meno nel primo round dello scontro. */
    WEAKENING(true),

    /** Neutralità: non subisce il vantaggio di fazione dell'avversaria. */
    NEUTRALITY(false),

    /** Immunità: immune agli effetti dell'avversaria che agiscono sulla creatura. */
    IMMUNITY(false),

    /** Non Sequitur: annulla gli effetti dell'avversaria finché resta in campo. */
    NON_SEQUITUR(false),

    /** Culla: il suo giocatore non subisce danni ai punti vita. */
    CRADLE(false),

    /** Riflesso X: quando subisce danni in combattimento, l'avversaria subisce X danni alla difesa. */
    REFLECTION(true),

    /** Barriera: il primo colpo che subisce in combattimento è annullato. */
    BARRIER(false),

    /** Assorbimento X: quando infligge danni ai punti vita dell'avversario, il suo giocatore ne guadagna X. */
    ABSORPTION(true),

    /** Permanenza X: a fine round recupera X difesa, mai oltre quella iniziale. */
    PERMANENCE(true),

    /** Intensificazione X: a fine round +X attacco. */
    INTENSIFICATION(true),

    /** Miasma X: a fine round X danni ai punti vita dell'avversario. */
    MIASMA(true),

    /** Ninna nanna X: a fine round il suo giocatore guadagna X punti vita. */
    LULLABY(true),

    /** Ultimo respiro X: quando viene uccisa, la creatura che l'ha uccisa subisce X danni alla difesa. */
    LAST_BREATH(true),

    /** Terrore puro: quando uccide, danni ai punti vita pari alla difesa originale della creatura uccisa. */
    PURE_TERROR(false);

    private final boolean hasValue;

    EffectCode(boolean hasValue) {
        this.hasValue = hasValue;
    }

    /**
     * Dice se l'effetto usa il valore X, come {@code effects.has_value}.
     *
     * @return {@code true} per gli effetti con la X (es. Castigo 2)
     */
    public boolean hasValue() {
        return hasValue;
    }

    /**
     * Trova l'effetto di una carta e controlla che il valore X ci sia solo quando serve.
     *
     * <p>Un effetto sconosciuto ferma la partita: giocare la carta senza il suo effetto darebbe
     * un risultato sbagliato senza che nessuno se ne accorga.
     *
     * @param effect l'effetto della carta, non {@code null}
     * @return l'effetto del catalogo
     * @throws NullPointerException     se {@code effect} è {@code null}
     * @throws IllegalArgumentException se il codice non è nel catalogo, o se manca il valore X
     *                                  di un effetto che lo richiede
     */
    public static EffectCode of(CardEffect effect) {
        Objects.requireNonNull(effect, "effect");
        EffectCode code;
        try {
            code = valueOf(effect.code());
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("Il motore non conosce l'effetto " + effect.code(), unknown);
        }
        if (code.hasValue && effect.value() == null) {
            throw new IllegalArgumentException("L'effetto " + code + " ha bisogno del valore X");
        }
        return code;
    }
}
