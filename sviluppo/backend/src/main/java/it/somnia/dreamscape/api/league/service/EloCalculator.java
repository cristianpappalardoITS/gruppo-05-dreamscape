package it.somnia.dreamscape.api.league.service;

/**
 * Calcola come cambia il punteggio di rank dopo una partita classificata (Elo con K = 32, decisione 010).
 *
 * <p>Si calcola la variazione di chi ha avviato la partita, arrotondata all'intero più vicino, e l'avversario
 * riceve l'opposto: i punti in gioco restano gli stessi. Il pareggio vale mezzo punto, quindi con punteggi uguali
 * non cambia niente e con punteggi diversi guadagna chi è più indietro. Unica eccezione: il punteggio non scende
 * sotto 0 ({@code chk_users_rating}), quindi chi è vicino a 0 perde solo quello che ha.
 */
public final class EloCalculator {

    /**
     * Quanto si muove al massimo il punteggio in una partita.
     */
    public static final int K_FACTOR = 32;

    // Con 400 punti di differenza chi è avanti ha 10 volte le probabilità di vincere dell'altro (scala Elo)
    private static final double RATING_SCALE = 400.0;

    private EloCalculator() {
    }

    /**
     * Calcola le variazioni del punteggio dei due giocatori.
     *
     * @param challengerRating il punteggio di chi ha avviato la partita, prima della partita
     * @param opponentRating   il punteggio dell'avversario, prima della partita
     * @param challengerScore  il risultato di chi ha avviato la partita: 1 vittoria, 0.5 pareggio, 0 sconfitta
     * @return le variazioni da sommare ai due punteggi
     * @throws IllegalArgumentException se un punteggio è negativo o il risultato non è 0, 0.5 o 1
     */
    public static RatingChanges calculate(int challengerRating, int opponentRating, double challengerScore) {
        if (challengerRating < 0 || opponentRating < 0) {
            throw new IllegalArgumentException("Il punteggio di rank non può essere negativo");
        }
        if (challengerScore != 0 && challengerScore != 0.5 && challengerScore != 1) {
            throw new IllegalArgumentException("Il risultato della partita è 0, 0.5 o 1: " + challengerScore);
        }
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentRating - challengerRating) / RATING_SCALE));
        int change = (int) Math.round(K_FACTOR * (challengerScore - expectedScore));
        return new RatingChanges(Math.max(change, -challengerRating), Math.max(-change, -opponentRating));
    }

    /**
     * Le variazioni del punteggio dei due giocatori dopo una partita.
     *
     * @param challenger quanto cambia il punteggio di chi ha avviato la partita
     * @param opponent   quanto cambia il punteggio dell'avversario
     */
    public record RatingChanges(int challenger, int opponent) {
    }
}
