package it.somnia.dreamscape.api.user.repository;

import it.somnia.dreamscape.api.user.model.CreditTransactionType;

// I movimenti di un tipo sommati: quanti sono, i crediti entrati e quelli usciti (Dashboard admin)
public interface CreditTotalsView {

    CreditTransactionType getTransactionType();

    long getTransactions();

    long getEarned();

    long getSpent();
}
