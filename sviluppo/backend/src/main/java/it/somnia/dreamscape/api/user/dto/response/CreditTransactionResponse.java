package it.somnia.dreamscape.api.user.dto.response;

import java.time.Instant;

import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;

// Una riga della pagina Crediti (GET /me/credit-transactions)
public record CreditTransactionResponse(CreditTransactionType type, int amount, int balanceAfter, Instant createdAt) {

    public static CreditTransactionResponse from(CreditTransaction transaction) {
        return new CreditTransactionResponse(transaction.getType(), transaction.getAmount(),
                transaction.getBalanceAfter(), transaction.getCreatedAt());
    }
}
