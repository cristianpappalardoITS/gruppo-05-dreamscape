package it.somnia.dreamscape.api.card.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.CardVariantType;

// I filtri del catalogo (GET /cards) sono in CardSpecifications
public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByCode(String code);

    // Nome già usato da un'altra carta (uq_cards_name), per il backoffice
    Optional<Card> findByName(String name);

    // Le carte normali attive (senza le versioni), es. per il catalogo pubblico
    List<Card> findByBaseCardIsNullAndActiveTrueOrderByCodeAsc();

    // Le carte filtrate con quelle collegate già caricate: una pagina del backoffice o del catalogo
    // non fa una query per ogni carta per leggere fazione, rarità, espansione e carta base
    @Override
    @EntityGraph(attributePaths = { "baseCard", "faction", "rarity", "cardSet" })
    Page<Card> findAll(Specification<Card> specification, Pageable pageable);

    // Le versioni (oggi solo la full art) di più carte con una sola query
    List<Card> findByBaseCardIn(Collection<Card> baseCards);

    // Una versione precisa, es. la full art che esce dal pacchetto al posto della carta normale
    Optional<Card> findByBaseCardAndVariantType(Card baseCard, CardVariantType variantType);
}
