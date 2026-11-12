package it.somnia.dreamscape.api.pack.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.deck.model.UserCards;
import it.somnia.dreamscape.api.deck.repository.UserCardsRepository;
import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackCover;
import it.somnia.dreamscape.api.pack.model.PackOpening;
import it.somnia.dreamscape.api.pack.model.PackOpeningCard;
import it.somnia.dreamscape.api.pack.model.PackOpeningSource;
import it.somnia.dreamscape.api.pack.repository.PackOpeningRepository;
import it.somnia.dreamscape.api.user.model.User;
import lombok.RequiredArgsConstructor;

/**
 * Apre un pacchetto per un utente: estrae le carte, salva l'apertura e aggiunge le carte alla collezione.
 *
 * <p>Lo usano sia l'acquisto nel negozio sia il Benvenuto. I crediti non sono qui: li scala chi compra.
 */
@Component
@RequiredArgsConstructor
public class PackOpener {

    private final PackDrawer packDrawer;
    private final PackOpeningRepository packOpeningRepository;
    private final UserCardsRepository userCardsRepository;

    /**
     * Apre un pacchetto dentro la transazione di chi lo chiama.
     *
     * @param lockedUser utente con la riga già bloccata nella transazione in corso
     * @param pack       il pacchetto da aprire
     * @param cover      la copertina scelta, o {@code null}
     * @param source     comprato ({@code PURCHASE}) o regalato ({@code SIGNUP})
     * @param pricePaid  i crediti pagati, 0 per i regali
     * @return l'apertura salvata, con le carte nell'ordine in cui sono uscite
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PackOpening open(User lockedUser, Pack pack, PackCover cover, PackOpeningSource source, int pricePaid) {
        List<Card> cards = packDrawer.draw(pack);
        PackOpening opening = new PackOpening(lockedUser, pack, cover, source, pricePaid);
        cards.forEach(opening::addCard);
        packOpeningRepository.save(opening);
        addToCollection(lockedUser, cards);
        return opening;
    }

    /**
     * Restituisce le carte di un'apertura, nell'ordine in cui sono uscite.
     *
     * @param opening l'apertura
     * @return le carte; per una full art c'è la carta della full art
     */
    public static List<Card> cardsOf(PackOpening opening) {
        return opening.getCards().stream().map(PackOpeningCard::getCard).toList();
    }

    // Una riga per carta posseduta: se c'è già si aumentano le copie, altrimenti si crea.
    // La full art è una carta a parte, quindi ha la sua riga.
    private void addToCollection(User user, List<Card> cards) {
        Map<UUID, UserCards> owned = new HashMap<>();
        userCardsRepository.findUserCardsByUser_Id(user.getId())
                .forEach(userCard -> owned.put(userCard.getCard().getId(), userCard));
        for (Card card : cards) {
            UserCards userCard = owned.get(card.getId());
            if (userCard != null) {
                userCard.setQuantity(userCard.getQuantity() + 1);
            } else {
                owned.put(card.getId(), userCardsRepository.save(new UserCards(user, card, 1)));
            }
        }
    }
}
