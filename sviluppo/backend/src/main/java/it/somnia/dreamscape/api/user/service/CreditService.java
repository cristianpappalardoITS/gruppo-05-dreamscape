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
 *
 * <p>Chi lo chiama ha già bloccato la riga dell'utente con
 * {@link UserService#getActiveUserForUpdate(UUID)}, così due operazioni insieme non sbagliano il saldo.
 * Unica eccezione: la registrazione, dove l'utente è appena stato creato nella stessa transazione
 * e nessun'altra richiesta può ancora vederlo.
 */
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditTransactionRepository creditTransactionRepository;

    /**
     * Aggiunge (amount positivo) o toglie (negativo) Dream Credits.
     *
     * @param lockedUser    utente con la riga già bloccata nella transazione in corso, o appena creato in essa
     * @param type          il motivo del movimento (es. {@code SIGNUP_BONUS}, {@code DAILY_REWARD})
     * @param amount        i crediti da aggiungere (positivo) o togliere (negativo), mai 0
     * @param packOpeningId apertura del pacchetto comprato, o null
     * @param matchId       partita che dà il premio, o null
     * @return il movimento salvato, con il saldo dopo l'operazione
     * @throws NotEnoughCreditsException se il saldo andrebbe sotto 0
     * @throws IllegalArgumentException  se {@code amount} è 0
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

    /**
     * Come {@link #apply(User, CreditTransactionType, int, UUID, UUID)}, per i movimenti senza
     * pacchetto né partita (es. bonus di registrazione e ricompensa giornaliera).
     *
     * @param lockedUser utente con la riga già bloccata nella transazione in corso, o appena creato in essa
     * @param type       il motivo del movimento
     * @param amount     i crediti da aggiungere (positivo) o togliere (negativo), mai 0
     * @return il movimento salvato, con il saldo dopo l'operazione
     * @throws NotEnoughCreditsException se il saldo andrebbe sotto 0
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CreditTransaction apply(User lockedUser, CreditTransactionType type, int amount) {
        return apply(lockedUser, type, amount, null, null);
    }

    /**
     * Restituisce i movimenti di crediti dell'utente, dal più recente ({@code GET /me/credit-transactions}).
     *
     * @param userId l'utente collegato
     * @param page   la pagina, da 0
     * @param size   quanti movimenti per pagina
     * @return la pagina dei movimenti
     */
    @Transactional(readOnly = true)
    public PageResponse<CreditTransactionResponse> listTransactions(UUID userId, int page, int size) {
        return PageResponse.from(
                creditTransactionRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size)),
                CreditTransactionResponse::from);
    }
}
