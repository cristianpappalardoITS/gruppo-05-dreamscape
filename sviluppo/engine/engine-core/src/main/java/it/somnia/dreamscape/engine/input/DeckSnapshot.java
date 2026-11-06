package it.somnia.dreamscape.engine.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Il mazzo di un giocatore com'era al momento della partita.
 *
 * <p>Il motore non controlla le regole del mazzo (20 carte, copie, rarità): lo fa il backend
 * prima della partita. Qui si rifiutano solo i dati rotti, come un mazzo vuoto.
 *
 * @param cards le carte del mazzo, ognuna con la sua quantità; non vuota
 */
public record DeckSnapshot(List<CardSnapshot> cards) {

    /**
     * Controlla le carte e rende la lista non modificabile.
     *
     * @throws NullPointerException     se la lista o una carta sono {@code null}
     * @throws IllegalArgumentException se il mazzo è vuoto
     */
    public DeckSnapshot {
        cards = List.copyOf(Objects.requireNonNull(cards, "cards"));
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("Il mazzo è vuoto");
        }
    }

    /**
     * Restituisce una carta per ogni copia, nell'ordine della lista.
     *
     * <p>Una carta con quantità 2 compare due volte, una dopo l'altra: è la lista <b>prima</b> del
     * mescolamento. {@code MatchSetup} poi mescola tutte le copie, ognuna per conto suo, quindi nel mazzo
     * di gioco le due copie finiscono in posizioni a caso. Questo ordine di partenza serve solo perché,
     * con lo stesso mazzo e lo stesso seed, il mescolamento dia sempre lo stesso risultato.
     *
     * @return la lista non modificabile di tutte le copie
     */
    public List<CardSnapshot> expandCopies() {
        List<CardSnapshot> copies = new ArrayList<>();
        for (CardSnapshot card : cards) {
            for (int copy = 0; copy < card.quantity(); copy++) {
                copies.add(card);
            }
        }
        return List.copyOf(copies);
    }
}
