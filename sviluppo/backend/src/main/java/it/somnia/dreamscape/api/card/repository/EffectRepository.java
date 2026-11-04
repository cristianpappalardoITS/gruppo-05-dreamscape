package it.somnia.dreamscape.api.card.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.card.model.Effect;

public interface EffectRepository extends JpaRepository<Effect, UUID> {

    Optional<Effect> findByCode(String code);

    // Solo gli effetti accesi si possono assegnare alle carte
    List<Effect> findByActiveTrueOrderByNameAsc();
}
