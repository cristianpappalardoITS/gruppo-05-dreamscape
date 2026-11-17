package it.somnia.dreamscape.api.pack.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.pack.model.Pack;
import it.somnia.dreamscape.api.pack.model.PackCover;

public interface PackCoverRepository extends JpaRepository<PackCover, UUID> {

    List<PackCover> findByPackOrderBySortOrderAsc(Pack pack);
}
