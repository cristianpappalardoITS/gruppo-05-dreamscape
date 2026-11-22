package it.somnia.dreamscape.api.card.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.card.model.Card;
import it.somnia.dreamscape.api.card.model.Faction;

public interface FactionRepository extends JpaRepository<Faction, UUID> {

    Optional<Faction> findByCode(String code);

    List<Faction> findAllByOrderByCodeAsc();

    // La fazione che ha questa carta come Lord (uq_factions_lord_card: al massimo una)
    Optional<Faction> findByLordCard(Card lordCard);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
