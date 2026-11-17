package it.somnia.dreamscape.api.trade.model;

// Uno scambio nasce PENDING e si chiude una volta sola: accettato, rifiutato o annullato. Non scade (decisione 009)
public enum TradeStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}
