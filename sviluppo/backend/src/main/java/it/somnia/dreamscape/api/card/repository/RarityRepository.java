package it.somnia.dreamscape.api.card.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.card.model.Rarity;

public interface RarityRepository extends JpaRepository<Rarity, UUID> {

    Optional<Rarity> findByCode(String code);

    // Dalla Comune alla Leggendaria
    List<Rarity> findAllByOrderBySortOrderAsc();
}
