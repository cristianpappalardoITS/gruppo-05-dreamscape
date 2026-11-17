package it.somnia.dreamscape.api.trade.repository;

import java.util.UUID;

// Copie di una carta già offerte da un giocatore negli scambi in attesa
public interface OfferedCopiesView {

    UUID getCardId();

    long getQuantity();
}
