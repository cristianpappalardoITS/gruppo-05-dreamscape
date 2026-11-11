package it.somnia.dreamscape.api.pack.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.pack.model.PackOpening;

public interface PackOpeningRepository extends JpaRepository<PackOpening, UUID> {

    // Un'apertura solo se è dell'utente: quella di un altro risulta inesistente
    Optional<PackOpening> findByIdAndUserId(UUID id, UUID userId);
}
