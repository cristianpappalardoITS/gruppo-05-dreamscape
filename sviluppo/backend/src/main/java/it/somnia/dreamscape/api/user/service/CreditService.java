package it.somnia.dreamscape.api.user.service;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.common.dto.response.PageResponse;
import it.somnia.dreamscape.api.user.dto.response.CreditTransactionResponse;
import it.somnia.dreamscape.api.user.model.CreditTransaction;
import it.somnia.dreamscape.api.user.model.CreditTransactionType;
import it.somnia.dreamscape.api.user.model.User;
import it.somnia.dreamscape.api.user.repository.CreditTransactionRepository;
import it.somnia.dreamscape.api.user.service.UserExceptions.NotEnoughCreditsException;
import lombok.RequiredArgsConstructor;

/**
 * Unico punto in cui cambia il saldo: aggiorna {@code users.credits} e scrive il movimento.
 * Chi lo chiama ha già bloccato la riga dell'utente ({@code UserRepository.findByIdForUpdate}).
 */
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditTransactionRepository creditTransactionRepository;

    /**
     * Aggiunge (amount positivo) o toglie (negativo) Dream Credits.
     *
     * @param lockedUser    utente con la riga già bloccata nella transazione in corso
     * @param packOpeningId apertura del pacchetto comprato, o null
     * @param matchId       partita che dà il premio, o null
     * @throws NotEnoughCreditsException se il saldo andrebbe sotto 0
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CreditTransaction apply(User lockedUser, CreditTransactionType type, int amount, UUID packOpeningId,
            UUID matchId) {
        if (amount == 0) {
            throw new IllegalArgumentException("Un movimento di crediti non può essere 0");
        }
        int balanceAfter = lockedUser.getCredits() + amount;
        if (balanceAfter < 0) {
            throw new NotEnoughCreditsException();
        }
        lockedUser.setCredits(balanceAfter);
        CreditTransaction transaction = new CreditTransaction(lockedUser, type, amount, balanceAfter);
        transaction.setPackOpeningId(packOpeningId);
        transaction.setMatchId(matchId);
        return creditTransactionRepository.save(transaction);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public CreditTransaction apply(User lockedUser, CreditTransactionType type, int amount) {
        return apply(lockedUser, type, amount, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<CreditTransactionResponse> listTransactions(UUID userId, int page, int size) {
        return PageResponse.from(
                creditTransactionRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size)),
                CreditTransactionResponse::from);
    }
}
