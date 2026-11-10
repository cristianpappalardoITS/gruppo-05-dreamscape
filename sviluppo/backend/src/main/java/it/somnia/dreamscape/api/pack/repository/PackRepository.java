package it.somnia.dreamscape.api.pack.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackType;

public interface PackRepository extends JpaRepository<Pack, UUID> {

    // I pacchetti in vendita in questo momento, dal più economico; stesse regole di Pack.isOnSale
    @Query("""
            SELECT p FROM Pack p
            WHERE p.packType = :type AND p.active = TRUE
              AND (p.availableFrom IS NULL OR p.availableFrom <= :now)
              AND (p.availableUntil IS NULL OR p.availableUntil > :now)
            ORDER BY p.priceCredits ASC, p.name ASC""")
    List<Pack> findOnSale(@Param("type") PackType type, @Param("now") Instant now);
}
