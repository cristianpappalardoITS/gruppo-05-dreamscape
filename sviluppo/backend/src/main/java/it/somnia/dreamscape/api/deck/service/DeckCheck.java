package it.somnia.dreamscape.api.deck.service;

import java.util.List;

import it.somnia.dreamscape.api.deck.model.Deck;
import it.somnia.dreamscape.api.deck.model.DeckCards;

/**
 * Un mazzo controllato adesso, per giocarlo: le sue carte in ordine di codice e i problemi di oggi.
 *
 * @param deck     il mazzo
 * @param cards    le carte con le copie, in ordine di codice
 * @param problems i problemi da mostrare al giocatore; lista vuota se il mazzo si può giocare
 */
public record DeckCheck(Deck deck, List<DeckCards> cards, List<String> problems) {

    public DeckCheck {
        cards = List.copyOf(cards);
        problems = List.copyOf(problems);
    }

    public boolean isValid() {
        return problems.isEmpty();
    }
}
