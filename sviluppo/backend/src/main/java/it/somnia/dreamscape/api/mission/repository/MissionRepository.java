package it.somnia.dreamscape.api.mission.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import it.somnia.dreamscape.api.mission.model.Mission;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

    // Le missioni attive: avanzano con le partite e si vedono nella Dashboard
    List<Mission> findByActiveTrue();
}
