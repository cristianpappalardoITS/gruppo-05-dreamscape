package it.somnia.dreamscape.api.card.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;

public interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findByCode(String code);

    // Le carte normali attive (senza le versioni), es. per il catalogo pubblico
    List<Card> findByBaseCardIsNullAndActiveTrueOrderByCodeAsc();

    // Tutte le versioni di una carta (oggi solo la full art)
    List<Card> findByBaseCard(Card baseCard);

    // Una versione precisa, es. la full art che esce dal pacchetto al posto della carta normale
    Optional<Card> findByBaseCardAndVariantType(Card baseCard, CardVariantType variantType);
}
