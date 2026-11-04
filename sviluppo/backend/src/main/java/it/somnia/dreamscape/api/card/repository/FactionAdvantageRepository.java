package it.somnia.dreamscape.api.card.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.card.model.FactionAdvantage;

public interface FactionAdvantageRepository extends JpaRepository<FactionAdvantage, UUID> {

    // Carica anche le due fazioni nella stessa query
    @EntityGraph(attributePaths = { "faction", "targetFaction" })
    List<FactionAdvantage> findAllByOrderByIdAsc();
}
