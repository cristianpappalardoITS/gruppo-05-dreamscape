package it.somnia.dreamscape.api.pack.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.pack.model.PackOpening;
import it.somnia.dreamscape.api.pack.model.PackOpeningSource;

public interface PackOpeningRepository extends JpaRepository<PackOpening, UUID> {

    // Un'apertura solo se è dell'utente: quella di un altro risulta inesistente
    Optional<PackOpening> findByIdAndUserId(UUID id, UUID userId);

    // Le aperture regalate (set iniziale e benvenuto), nell'ordine in cui sono state fatte
    List<PackOpening> findByUserIdAndSourceOrderByIdAsc(UUID userId, PackOpeningSource source);
}
