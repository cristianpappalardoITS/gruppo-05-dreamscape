package it.somnia.dreamscape.api.match.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.match.model.GameMode;

public interface GameModeRepository extends JpaRepository<GameMode, UUID> {

    Optional<GameMode> findByCode(String code);
}
