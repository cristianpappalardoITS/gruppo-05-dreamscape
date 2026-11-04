package it.somnia.dreamscape.api.card.model;

// Quando agisce un effetto, salvato come testo in effects.trigger_event
public enum EffectTrigger {
    // Quando la carta viene giocata (fase iniziale)
    ON_PLAY,
    // Finché la carta resta in campo
    WHILE_IN_PLAY,
    // Alla fine di ogni round, se la carta è ancora in campo
    END_OF_ROUND,
    // Quando la carta viene uccisa
    ON_DEATH,
    // Quando la carta uccide una creatura avversaria
    ON_KILL
}
